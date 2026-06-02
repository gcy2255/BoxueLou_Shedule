#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
解析博学楼所有课表Excel，生成MySQL INSERT语句
"""

import xlrd
import re
import os
import sys

EXCEL_DIR = os.path.dirname(os.path.abspath(__file__))

# 正则：课程名/(节次)周次信息/教师/班级/教学班人数：XX
# 周次信息可能是逗号分隔的多段，如：2-4周(双),8-16周(双) 或 1-2周,4-13周,15-16周
COURSE_RE = re.compile(
    r'([^/]+?)/\((\d+)-(\d+)节\)([^/]+)/([^/]+)/([^/]+)/教学班人数[：:](\d+)'
)

# 正则：借用 审核状态：借用审核通过/借用原因:xxx/(节次)周次信息/负责人
BORROW_RE = re.compile(
    r'审核状态[：:].*?借用原因[:：](.*?)/\((\d+)-(\d+)节\)([^/]+)/(.+)'
)

# 解析单段周次范围（带类型标记）：如 "1-10周(双)" 或 "2-16(单)"
WEEK_TYPED_RE = re.compile(r'(\d+(?:-\d+)?)周?\((单|双)\)')
# 解析单段周次范围（无类型标记）：如 "1-10周" 或 "10"
WEEK_PLAIN_RE = re.compile(r'(\d+(?:-\d+)?)周?')

SEAT_RE = re.compile(r'座位数[：:]?(\d+)')


def escape_sql(s):
    """转义SQL字符串"""
    if s is None:
        return 'NULL'
    s = str(s).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"


def parse_week_info(week_str):
    """
    解析周次信息字符串，返回 [(week_start, week_end, week_type), ...] 列表
    支持格式：
      - 1-10周          → [(1,10,0)]
      - 2-16周(双)      → [(2,16,2)]
      - 1-15周(单)      → [(1,15,1)]
      - 10周            → [(10,10,0)]
      - 2-4周(双),8-16周(双) → [(2,4,2),(8,16,2)]
      - 1-2周,4-13周,15-16周 → [(1,2,0),(4,13,0),(15,16,0)]
    """
    results = []
    # 按逗号分割多段
    segments = week_str.split(',')
    for seg in segments:
        seg = seg.strip()
        if not seg:
            continue
        # 先尝试匹配带类型标记的：如 "1-10(双)" 或 "2-16周(单)"
        m = WEEK_TYPED_RE.search(seg)
        if m:
            week_range = m.group(1)
            type_str = m.group(2)
            if '-' in week_range:
                parts = week_range.split('-')
                ws, we = int(parts[0]), int(parts[1])
            else:
                ws = we = int(week_range)
            wt = 1 if type_str == '单' else 2
            results.append((ws, we, wt))
        else:
            # 无类型标记：如 "1-10周" 或 "10"
            m = WEEK_PLAIN_RE.search(seg)
            if m:
                week_range = m.group(1)
                if '-' in week_range:
                    parts = week_range.split('-')
                    ws, we = int(parts[0]), int(parts[1])
                else:
                    ws = we = int(week_range)
                results.append((ws, we, 0))
    return results


def parse_floor(room_name):
    """解析楼层号"""
    if 'B' in room_name:
        return -1
    if 'A' in room_name:
        return 0
    m = re.search(r'博学楼(\d)\d{2}', room_name)
    if m:
        return int(m.group(1))
    if '七楼' in room_name:
        return 7
    return 0


def parse_room_type(room_name):
    """解析房间类型"""
    if '实验室' in room_name or '计算机' in room_name:
        return '实验室'
    return '普通教室'


def parse_seat_count(sheet):
    """解析座位数"""
    row = sheet.row(0)
    for i in range(sheet.ncols):
        try:
            val = str(row[i].value).strip()
            m = SEAT_RE.search(val)
            if m:
                return int(m.group(1))
        except:
            pass
    return 0


def parse_excel(filepath, room_id):
    """解析单个Excel文件，返回 (room_info, schedules)"""
    filename = os.path.basename(filepath)
    room_name = filename.replace('.xls', '')

    try:
        wb = xlrd.open_workbook(filepath, formatting_info=False)
    except Exception as e:
        print(f"-- 错误：无法打开 {filename}: {e}", file=sys.stderr)
        return None, []

    sheet = wb.sheet_by_index(0)
    if sheet.nrows < 3:
        print(f"-- 警告：{filename} 行数不足", file=sys.stderr)
        return None, []

    seat_count = parse_seat_count(sheet)
    floor_num = parse_floor(room_name)
    room_type = parse_room_type(room_name)

    room_info = {
        'id': room_id,
        'room_name': room_name,
        'seat_count': seat_count,
        'floor_num': floor_num,
        'room_type': room_type,
    }

    # 行映射：row2=1-2节, row3=3-4节, row4=5-6节, row5=7-8节, row6=9-10节, row7=11-12节
    period_map = [(1,2),(3,4),(5,6),(7,8),(9,10),(11,12)]
    schedules = []

    for row_idx in range(2, 8):
        if row_idx >= sheet.nrows:
            break
        row = sheet.row(row_idx)
        period_start, period_end = period_map[row_idx - 2]

        for col_idx in range(2, 9):
            if col_idx >= sheet.ncols:
                break
            day_of_week = col_idx - 1  # 1=周一, 7=周日

            try:
                cell_val = str(row[col_idx].value).strip()
            except:
                continue
            if not cell_val:
                continue

            entries = re.split(r'\r?\n', cell_val)
            for entry in entries:
                entry = entry.strip()
                if not entry:
                    continue

                if '审核状态' in entry or ('借用' in entry and '借用原因' in entry):
                    # 借用记录
                    m = BORROW_RE.search(entry)
                    if m:
                        borrow_reason = m.group(1).strip()
                        ps, pe = int(m.group(2)), int(m.group(3))
                        week_str = m.group(4).strip()
                        teacher = m.group(5).strip()

                        week_list = parse_week_info(week_str)
                        if not week_list:
                            week_list = [(1, 18, 0)]  # 默认全学期

                        for ws, we, wt in week_list:
                            schedules.append({
                                'room_id': room_id,
                                'course_name': '借用',
                                'teacher': teacher,
                                'class_info': None,
                                'student_count': None,
                                'day_of_week': day_of_week,
                                'period_start': ps,
                                'period_end': pe,
                                'week_start': ws,
                                'week_end': we,
                                'week_type': wt,
                                'is_borrowed': 1,
                                'borrow_reason': borrow_reason,
                            })
                    else:
                        print(f"-- 警告：无法解析借用 [{room_name}] 行{row_idx}列{col_idx}: {entry[:80]}", file=sys.stderr)
                else:
                    # 普通课程
                    m = COURSE_RE.search(entry)
                    if m:
                        course_name = m.group(1).strip()
                        ps, pe = int(m.group(2)), int(m.group(3))
                        week_str = m.group(4).strip()
                        teacher = m.group(5).strip()
                        class_info = m.group(6).strip()
                        student_count = int(m.group(7))

                        week_list = parse_week_info(week_str)
                        if not week_list:
                            week_list = [(1, 18, 0)]

                        for ws, we, wt in week_list:
                            schedules.append({
                                'room_id': room_id,
                                'course_name': course_name,
                                'teacher': teacher,
                                'class_info': class_info,
                                'student_count': student_count,
                                'day_of_week': day_of_week,
                                'period_start': ps,
                                'period_end': pe,
                                'week_start': ws,
                                'week_end': we,
                                'week_type': wt,
                                'is_borrowed': 0,
                                'borrow_reason': None,
                            })
                    else:
                        print(f"-- 警告：无法解析课程 [{room_name}] 行{row_idx}列{col_idx}: {entry[:80]}", file=sys.stderr)

    return room_info, schedules


def main():
    excel_files = []
    for f in sorted(os.listdir(EXCEL_DIR)):
        if f.endswith('.xls') and not f.startswith('~'):
            excel_files.append(os.path.join(EXCEL_DIR, f))

    print(f"-- 共找到 {len(excel_files)} 个Excel文件")
    print()

    # 建库建表
    print("""CREATE DATABASE IF NOT EXISTS classroom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE classroom_db;

DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS room;

CREATE TABLE room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(50) NOT NULL COMMENT '教室名',
    seat_count INT COMMENT '座位数',
    floor_num INT COMMENT '楼层',
    room_type VARCHAR(20) COMMENT '类型',
    UNIQUE KEY uk_room_name (room_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

CREATE TABLE schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL COMMENT '教室ID',
    course_name VARCHAR(200) NOT NULL COMMENT '课程名',
    teacher VARCHAR(50) COMMENT '教师',
    class_info VARCHAR(500) COMMENT '班级信息',
    student_count INT COMMENT '教学班人数',
    day_of_week TINYINT NOT NULL COMMENT '1-7 周一~日',
    period_start TINYINT NOT NULL COMMENT '开始节次',
    period_end TINYINT NOT NULL COMMENT '结束节次',
    week_start INT NOT NULL COMMENT '开始周次',
    week_end INT NOT NULL COMMENT '结束周次',
    week_type TINYINT DEFAULT 0 COMMENT '0-每周 1-单周 2-双周',
    is_borrowed TINYINT DEFAULT 0 COMMENT '0-否 1-是',
    borrow_reason VARCHAR(500) COMMENT '借用原因',
    INDEX idx_room_id (room_id),
    INDEX idx_day_period (day_of_week, period_start, period_end),
    INDEX idx_week (week_start, week_end),
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
""")

    all_rooms = []
    all_schedules = []
    room_id = 1

    for filepath in excel_files:
        room_info, schedules = parse_excel(filepath, room_id)
        if room_info:
            all_rooms.append(room_info)
            all_schedules.extend(schedules)
            room_id += 1

    # room INSERT
    print(f"-- ========== 教室数据 ({len(all_rooms)} 个) ==========")
    print("INSERT INTO room (id, room_name, seat_count, floor_num, room_type) VALUES")
    room_values = []
    for r in all_rooms:
        room_values.append(
            f"({r['id']}, {escape_sql(r['room_name'])}, {r['seat_count']}, "
            f"{r['floor_num']}, {escape_sql(r['room_type'])})"
        )
    print(",\n".join(room_values) + ";")
    print()

    # schedule INSERT（分批500条）
    print(f"-- ========== 课程数据 ({len(all_schedules)} 条) ==========")
    batch_size = 500
    for batch_start in range(0, len(all_schedules), batch_size):
        batch = all_schedules[batch_start:batch_start + batch_size]
        print("INSERT INTO schedule (room_id, course_name, teacher, class_info, student_count, "
              "day_of_week, period_start, period_end, week_start, week_end, week_type, is_borrowed, borrow_reason) VALUES")
        sched_values = []
        for s in batch:
            sched_values.append(
                f"({s['room_id']}, {escape_sql(s['course_name'])}, {escape_sql(s['teacher'])}, "
                f"{escape_sql(s['class_info'])}, {s['student_count'] if s['student_count'] else 'NULL'}, "
                f"{s['day_of_week']}, {s['period_start']}, {s['period_end']}, "
                f"{s['week_start']}, {s['week_end']}, {s['week_type']}, "
                f"{s['is_borrowed']}, {escape_sql(s['borrow_reason'])})"
            )
        print(",\n".join(sched_values) + ";")
        print()

    print(f"-- 完成！共 {len(all_rooms)} 个教室，{len(all_schedules)} 条课程记录")


if __name__ == '__main__':
    main()
