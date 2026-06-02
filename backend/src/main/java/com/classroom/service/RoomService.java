package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classroom.entity.Room;
import com.classroom.entity.Schedule;
import com.classroom.mapper.RoomMapper;
import com.classroom.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomMapper roomMapper;
    private final ScheduleMapper scheduleMapper;

    /**
     * 学期开始日期（2025-2026学年第2学期）
     */
    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 3, 9);

    /**
     * 获取所有教室及当前状态
     */
    public List<Map<String, Object>> getAllRoomsWithStatus() {
        List<Room> rooms = roomMapper.selectList(null);
        int currentWeek = getCurrentWeek();
        int currentDay = LocalDate.now().getDayOfWeek().getValue(); // 1=周一, 7=周日
        int currentPeriod = getCurrentPeriod();

        return rooms.stream().map(room -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", room.getId());
            map.put("roomName", room.getRoomName());
            map.put("seatCount", room.getSeatCount());
            map.put("floorNum", room.getFloorNum());
            map.put("roomType", room.getRoomType());

            // 查询当前时间是否有课
            List<Schedule> currentSchedules = getCurrentSchedules(room.getId(), currentWeek, currentDay, currentPeriod);
            map.put("isOccupied", !currentSchedules.isEmpty());
            map.put("currentCourses", currentSchedules.stream()
                    .map(Schedule::getCourseName)
                    .collect(Collectors.toList()));

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索教室
     */
    public List<Map<String, Object>> searchRooms(String keyword) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Room::getRoomName, keyword);
        List<Room> rooms = roomMapper.selectList(wrapper);

        int currentWeek = getCurrentWeek();
        int currentDay = LocalDate.now().getDayOfWeek().getValue();
        int currentPeriod = getCurrentPeriod();

        return rooms.stream().map(room -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", room.getId());
            map.put("roomName", room.getRoomName());
            map.put("seatCount", room.getSeatCount());
            map.put("floorNum", room.getFloorNum());
            map.put("roomType", room.getRoomType());

            List<Schedule> currentSchedules = getCurrentSchedules(room.getId(), currentWeek, currentDay, currentPeriod);
            map.put("isOccupied", !currentSchedules.isEmpty());
            map.put("currentCourses", currentSchedules.stream()
                    .map(Schedule::getCourseName)
                    .collect(Collectors.toList()));

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取教室详情及课表
     */
    public Map<String, Object> getRoomDetail(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", room.getId());
        map.put("roomName", room.getRoomName());
        map.put("seatCount", room.getSeatCount());
        map.put("floorNum", room.getFloorNum());
        map.put("roomType", room.getRoomType());

        // 获取当前周次的课表
        int currentWeek = getCurrentWeek();
        List<Schedule> schedules = scheduleMapper.findByRoomAndWeek(roomId, currentWeek);

        // 按星期和节次组织课表
        Map<String, List<Map<String, Object>>> weeklySchedule = new LinkedHashMap<>();
        for (int day = 1; day <= 7; day++) {
            for (int period = 1; period <= 12; period += 2) {
                String key = day + "-" + period;
                int finalDay = day;
                int finalPeriod = period;
                List<Schedule> dayPeriodSchedules = schedules.stream()
                        .filter(s -> s.getDayOfWeek() == finalDay
                                && s.getPeriodStart() <= finalPeriod
                                && s.getPeriodEnd() >= finalPeriod)
                        .filter(s -> isInWeek(currentWeek, s))
                        .collect(Collectors.toList());

                if (!dayPeriodSchedules.isEmpty()) {
                    List<Map<String, Object>> courseList = dayPeriodSchedules.stream()
                            .map(s -> {
                                Map<String, Object> courseMap = new LinkedHashMap<>();
                                courseMap.put("courseName", s.getCourseName());
                                courseMap.put("teacher", s.getTeacher());
                                courseMap.put("classInfo", s.getClassInfo());
                                courseMap.put("studentCount", s.getStudentCount());
                                courseMap.put("isBorrowed", s.getIsBorrowed());
                                courseMap.put("borrowReason", s.getBorrowReason());
                                return courseMap;
                            })
                            .collect(Collectors.toList());
                    weeklySchedule.put(key, courseList);
                }
            }
        }

        map.put("weeklySchedule", weeklySchedule);
        map.put("currentWeek", currentWeek);

        return map;
    }

    /**
     * 查询教室指定时间状态
     */
    public Map<String, Object> getRoomStatus(Long roomId, Integer week, Integer day, Integer period) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            return null;
        }

        List<Schedule> schedules = getCurrentSchedules(roomId, week, day, period);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roomId", roomId);
        map.put("roomName", room.getRoomName());
        map.put("week", week);
        map.put("day", day);
        map.put("period", period);
        map.put("isOccupied", !schedules.isEmpty());
        map.put("courses", schedules.stream()
                .map(s -> {
                    Map<String, Object> courseMap = new LinkedHashMap<>();
                    courseMap.put("courseName", s.getCourseName());
                    courseMap.put("teacher", s.getTeacher());
                    courseMap.put("isBorrowed", s.getIsBorrowed());
                    return courseMap;
                })
                .collect(Collectors.toList()));

        return map;
    }

    /**
     * 获取当前时间段的课程
     */
    private List<Schedule> getCurrentSchedules(Long roomId, int week, int day, int period) {
        List<Schedule> schedules = scheduleMapper.findByRoomAndTime(roomId, day, period, week);
        return schedules.stream()
                .filter(s -> isInWeek(week, s))
                .collect(Collectors.toList());
    }

    /**
     * 判断当前周次是否在课程的周次范围内
     */
    private boolean isInWeek(int week, Schedule schedule) {
        if (week < schedule.getWeekStart() || week > schedule.getWeekEnd()) {
            return false;
        }

        int weekType = schedule.getWeekType();
        if (weekType == 0) {
            return true; // 每周
        } else if (weekType == 1) {
            return week % 2 == 1; // 单周
        } else {
            return week % 2 == 0; // 双周
        }
    }

    /**
     * 获取当前周次
     */
    public int getCurrentWeek() {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(SEMESTER_START, today);
        return (int) (days / 7) + 1;
    }

    /**
     * 获取当前节次（根据时间判断）
     *
     * 博学楼课表时间安排：
     * 第1节 8:00-8:45   第2节 8:55-9:40    → 1-2节 时段 8:00-9:40
     * 第3节 10:00-10:45 第4节 10:55-11:40  → 3-4节 时段 10:00-11:40
     * 第5节 14:20-15:05 第6节 15:15-16:00  → 5-6节 时段 14:20-16:00
     * 第7节 16:20-17:05 第8节 17:15-18:00  → 7-8节 时段 16:20-18:00
     * 第9节 19:20-20:05 第10节 20:10-20:55 → 9-10节 时段 19:20-20:55
     * 第11节 21:00-21:45 第12节 21:50-22:35 → 11-12节 时段 21:00-22:35（推算）
     */
    public int getCurrentPeriod() {
        int hour = java.time.LocalTime.now().getHour();
        int minute = java.time.LocalTime.now().getMinute();
        int time = hour * 100 + minute;

        if (time < 800)  return 0;  // 未上课
        if (time < 940)  return 1;  // 1-2节  8:00-9:40
        if (time < 1000) return 0;  // 课间休息 9:40-10:00
        if (time < 1140) return 3;  // 3-4节  10:00-11:40
        if (time < 1420) return 0;  // 午休   11:40-14:20
        if (time < 1600) return 5;  // 5-6节  14:20-16:00
        if (time < 1620) return 0;  // 课间休息 16:00-16:20
        if (time < 1800) return 7;  // 7-8节  16:20-18:00
        if (time < 1920) return 0;  // 晚饭   18:00-19:20
        if (time < 2055) return 9;  // 9-10节 19:20-20:55
        if (time < 2100) return 0;  // 课间休息 20:55-21:00
        if (time < 2235) return 11; // 11-12节 21:00-22:35
        return 0;                   // 下课后
    }

    /**
     * 获取当前时间信息
     */
    public Map<String, Object> getCurrentTimeInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("currentWeek", getCurrentWeek());
        info.put("currentDay", LocalDate.now().getDayOfWeek().getValue());
        info.put("currentPeriod", getCurrentPeriod());
        info.put("currentDate", LocalDate.now().toString());
        return info;
    }
}
