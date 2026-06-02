package com.classroom.service;

import com.classroom.entity.Room;
import com.classroom.entity.Schedule;
import com.classroom.mapper.RoomMapper;
import com.classroom.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelParserService implements CommandLineRunner {

    private final RoomMapper roomMapper;
    private final ScheduleMapper scheduleMapper;

    @Value("${classroom.excel-path}")
    private String excelPath;

    /**
     * 课程数据正则：课程名/(节次)周次/教师/班级/人数
     * Group 1: 课程名 (如 遗传学)
     * Group 2: 开始节次 (如 1)
     * Group 3: 结束节次 (如 2)
     * Group 4: 周次范围 (如 1-10 或 10)
     * Group 5: 周次类型 (单/双/null)
     * Group 6: 教师
     * Group 7: 班级
     *
     * 示例：遗传学/(1-2节)1-10周/余姣君/生科202301;生科202302/教学班人数：58
     * 示例：博弈论基础/(1-2节)2-16周(双)/马国锋/大数据202401/教学班人数：42
     */
    private static final Pattern COURSE_PATTERN = Pattern.compile(
            "([^/]+?)\\/\\((\\d+)-(\\d+)节\\)(\\d+(?:-\\d+)?)(?:周\\((单|双)\\))?周?\\/([^/]+)\\/([^/]+)\\/教学班人数[：:]\\d+");

    /**
     * 借用数据正则
     * Group 1: 借用原因
     * Group 2: 开始节次
     * Group 3: 结束节次
     * Group 4: 周次范围
     * Group 5: 周次类型
     * Group 6: 负责人
     *
     * 示例：审核状态：借用审核通过/借用原因:用于答辩-xxx/(1-2节)10周/罗彬
     */
    private static final Pattern BORROW_PATTERN = Pattern.compile(
            "审核状态[：:].+?借用原因[:：](.+?)\\/\\((\\d+)-(\\d+)节\\)(\\d+(?:-\\d+)?)(?:周\\((单|双)\\))?周?\\/(.+)");

    /**
     * 周次正则：支持 1-10周、1-15周(单)、2-16周(双)、10周（单周）
     */
    private static final Pattern WEEK_PATTERN = Pattern.compile("(\\d+)(?:-(\\d+))?周(?:\\((单|双)\\))?");

    private static final Pattern SEAT_PATTERN = Pattern.compile("座位数[：:]?(\\d+)");

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已有数据
        if (roomMapper.selectCount(null) > 0) {
            log.info("数据库已有数据，跳过Excel解析");
            return;
        }

        log.info("开始解析Excel文件...");
        Path path = Paths.get(excelPath).toAbsolutePath().normalize();
        log.info("Excel路径: {}", path);

        if (!Files.exists(path)) {
            log.error("Excel路径不存在: {}", path);
            return;
        }

        int[] count = {0};
        try (Stream<Path> files = Files.list(path)) {
            files.filter(p -> p.toString().endsWith(".xls"))
                    .forEach(p -> {
                        parseExcelFile(p);
                        count[0]++;
                    });
        }

        log.info("Excel解析完成，共处理 {} 个文件，导入 {} 个教室", count[0], roomMapper.selectCount(null));
    }

    @Transactional
    public void parseExcelFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String roomName = fileName.replace(".xls", "");

        log.info("解析文件: {}", roomName);

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                log.warn("文件 {} 没有工作表", fileName);
                return;
            }

            // 解析座位数
            int seatCount = parseSeatCount(sheet);

            // 解析楼层
            int floorNum = parseFloorNum(roomName);

            // 解析房间类型
            String roomType = parseRoomType(roomName);

            // 保存教室
            Room room = new Room();
            room.setRoomName(roomName);
            room.setSeatCount(seatCount);
            room.setFloorNum(floorNum);
            room.setRoomType(roomType);
            roomMapper.insert(room);

            // 解析课表
            List<Schedule> schedules = parseSchedule(sheet, room.getId());
            for (Schedule schedule : schedules) {
                scheduleMapper.insert(schedule);
            }

            log.info("成功导入教室 {}，座位数 {}，课程数 {}", roomName, seatCount, schedules.size());

        } catch (IOException e) {
            log.error("解析文件 {} 失败: {}", fileName, e.getMessage(), e);
        } catch (Exception e) {
            log.error("处理文件 {} 异常: {}", fileName, e.getMessage(), e);
        }
    }

    /**
     * 解析座位数 - 遍历第一行所有单元格（包括合并单元格）查找"座位数：xxx"
     */
    private int parseSeatCount(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row == null) return 0;

        // 先遍历实际存在的单元格
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;

            String value = getCellValue(cell).trim();
            if (!value.isEmpty()) {
                Matcher matcher = SEAT_PATTERN.matcher(value);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }

        // 如果遍历没找到，检查合并单元格区域（座位数通常在col 7-8的合并区域）
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() == 0) {
                Cell cell = sheet.getRow(region.getFirstRow()).getCell(region.getFirstColumn());
                if (cell != null) {
                    String value = getCellValue(cell).trim();
                    if (!value.isEmpty()) {
                        Matcher matcher = SEAT_PATTERN.matcher(value);
                        if (matcher.find()) {
                            return Integer.parseInt(matcher.group(1));
                        }
                    }
                }
            }
        }

        return 0;
    }

    private int parseFloorNum(String roomName) {
        // 博学楼102 -> 1, 博学楼A01 -> 0 (A层), 博学楼B01 -> -1 (B层)
        if (roomName.contains("B")) return -1;
        if (roomName.contains("A")) return 0;

        // 提取博学楼后面的数字，取第一位作为楼层
        Pattern pattern = Pattern.compile("博学楼(\\d)\\d{2}");
        Matcher matcher = pattern.matcher(roomName);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        // 尝试提取末尾数字（处理实验室命名如"计算机公共课实验室（一）（博学楼七楼东）"）
        if (roomName.contains("七楼")) return 7;

        return 0;
    }

    private String parseRoomType(String roomName) {
        if (roomName.contains("实验室") || roomName.contains("计算机")) {
            return "实验室";
        }
        return "普通教室";
    }

    /**
     * 解析课表 - 遍历行2-7、列2-8
     */
    private List<Schedule> parseSchedule(Sheet sheet, Long roomId) {
        List<Schedule> schedules = new ArrayList<>();

        // 行映射：row 2-7 对应时间段
        // row 2: 上午 第一节 (1-2节)
        // row 3: 上午 第二节 (3-4节)
        // row 4: 下午 第三节 (5-6节)
        // row 5: 下午 第四节 (7-8节)
        // row 6: 晚上 第五节 (9-10节)
        // row 7: 晚上 第六节 (11-12节)
        int[][] periodMap = {
                {1, 2},   // row 2
                {3, 4},   // row 3
                {5, 6},   // row 4
                {7, 8},   // row 5
                {9, 10},  // row 6
                {11, 12}  // row 7
        };

        for (int rowIdx = 2; rowIdx <= 7; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;

            // 列映射：col 2-8 对应周一到周日
            for (int colIdx = 2; colIdx <= 8; colIdx++) {
                Cell cell = row.getCell(colIdx);
                if (cell == null) continue;

                String value = getCellValue(cell).trim();
                if (value.isEmpty()) continue;

                int dayOfWeek = colIdx - 1; // 1=周一, 7=周日
                int periodStart = periodMap[rowIdx - 2][0];
                int periodEnd = periodMap[rowIdx - 2][1];

                // 按换行分割多个课程（处理 \r\n 和 \n）
                String[] entries = value.split("\\r?\\n");
                for (String entry : entries) {
                    entry = entry.trim();
                    if (entry.isEmpty()) continue;

                    try {
                        Schedule schedule = null;
                        if (entry.contains("审核状态") || entry.contains("借用")) {
                            schedule = parseBorrowEntry(entry, roomId, dayOfWeek, periodStart, periodEnd);
                        } else {
                            schedule = parseCourseEntry(entry, roomId, dayOfWeek, periodStart, periodEnd);
                        }

                        if (schedule != null) {
                            schedules.add(schedule);
                        }
                    } catch (Exception e) {
                        log.warn("解析条目失败 [{}][行{}列{}]: {} - {}", roomMapper.selectById(roomId) != null ?
                                roomMapper.selectById(roomId).getRoomName() : "?", rowIdx, colIdx, entry, e.getMessage());
                    }
                }
            }
        }

        return schedules;
    }

    /**
     * 解析普通课程条目
     * 格式：课程名/(节次)周次/教师/班级/教学班人数：XX
     *
     * COURSE_PATTERN groups:
     *   1=课程名, 2=开始节次, 3=结束节次, 4=周次范围, 5=周次类型, 6=教师, 7=班级
     */
    private Schedule parseCourseEntry(String entry, Long roomId, int dayOfWeek, int periodStart, int periodEnd) {
        Matcher matcher = COURSE_PATTERN.matcher(entry);
        if (!matcher.find()) {
            log.warn("无法解析课程: {}", entry);
            return null;
        }

        Schedule schedule = new Schedule();
        schedule.setRoomId(roomId);
        schedule.setCourseName(matcher.group(1).trim());
        schedule.setPeriodStart(Integer.parseInt(matcher.group(2)));
        schedule.setPeriodEnd(Integer.parseInt(matcher.group(3)));
        schedule.setTeacher(matcher.group(6).trim());
        schedule.setClassInfo(matcher.group(7).trim());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setIsBorrowed(0);

        // 解析学生人数
        Matcher countMatcher = Pattern.compile("教学班人数[：:](\\d+)").matcher(entry);
        if (countMatcher.find()) {
            schedule.setStudentCount(Integer.parseInt(countMatcher.group(1)));
        }

        // 解析周次: group4=范围(如"1-10"), group5=类型(单/双/null)
        parseWeekRange(matcher.group(4), matcher.group(5), schedule);

        return schedule;
    }

    /**
     * 解析借用条目
     * 格式：审核状态：借用审核通过/借用原因:xxx/(节次)周次/负责人
     *
     * BORROW_PATTERN groups:
     *   1=借用原因, 2=开始节次, 3=结束节次, 4=周次范围, 5=周次类型, 6=负责人
     */
    private Schedule parseBorrowEntry(String entry, Long roomId, int dayOfWeek, int periodStart, int periodEnd) {
        Matcher matcher = BORROW_PATTERN.matcher(entry);
        if (!matcher.find()) {
            log.warn("无法解析借用: {}", entry);
            return null;
        }

        Schedule schedule = new Schedule();
        schedule.setRoomId(roomId);
        schedule.setCourseName("借用");
        schedule.setPeriodStart(Integer.parseInt(matcher.group(2)));
        schedule.setPeriodEnd(Integer.parseInt(matcher.group(3)));
        schedule.setTeacher(matcher.group(6).trim());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setIsBorrowed(1);
        schedule.setBorrowReason(matcher.group(1).trim());

        // 解析周次: group4=范围, group5=类型
        parseWeekRange(matcher.group(4), matcher.group(5), schedule);

        return schedule;
    }

    /**
     * 解析周次范围
     * @param weekRange 周次范围部分，如 "1-10" 或 "10"
     * @param weekTypeStr 周次类型部分，如 "单"、"双" 或 null
     */
    private void parseWeekRange(String weekRange, String weekTypeStr, Schedule schedule) {
        // 处理 "1-10" 格式
        if (weekRange.contains("-")) {
            String[] parts = weekRange.split("-");
            schedule.setWeekStart(Integer.parseInt(parts[0]));
            schedule.setWeekEnd(Integer.parseInt(parts[1]));
        } else {
            // 单周 "10"
            int week = Integer.parseInt(weekRange);
            schedule.setWeekStart(week);
            schedule.setWeekEnd(week);
        }

        if (weekTypeStr == null) {
            schedule.setWeekType(0); // 每周
        } else if ("单".equals(weekTypeStr)) {
            schedule.setWeekType(1); // 单周
        } else if ("双".equals(weekTypeStr)) {
            schedule.setWeekType(2); // 双周
        } else {
            schedule.setWeekType(0); // 默认每周
        }
    }

    /**
     * 获取单元格值，处理各种类型
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // 处理整数（避免显示 .0）
                double numVal = cell.getNumericCellValue();
                if (numVal == (int) numVal) {
                    return String.valueOf((int) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        double formulaVal = cell.getNumericCellValue();
                        if (formulaVal == (int) formulaVal) {
                            return String.valueOf((int) formulaVal);
                        }
                        return String.valueOf(formulaVal);
                    } catch (Exception e2) {
                        return "";
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
