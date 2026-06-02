-- 创建数据库
CREATE DATABASE IF NOT EXISTS classroom_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE classroom_db;

-- 教室表
CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(50) NOT NULL COMMENT '教室名（如博学楼102）',
    seat_count INT COMMENT '座位数',
    floor_num INT COMMENT '楼层',
    room_type VARCHAR(20) COMMENT '类型：普通教室/实验室',
    UNIQUE KEY uk_room_name (room_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室表';

-- 课程表
CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL COMMENT '教室ID',
    course_name VARCHAR(200) NOT NULL COMMENT '课程名',
    teacher VARCHAR(50) COMMENT '教师',
    class_info VARCHAR(500) COMMENT '班级信息',
    student_count INT COMMENT '教学班人数',
    day_of_week TINYINT NOT NULL COMMENT '1-7 星期一~日',
    period_start TINYINT NOT NULL COMMENT '开始节次',
    period_end TINYINT NOT NULL COMMENT '结束节次',
    week_start INT NOT NULL COMMENT '开始周次',
    week_end INT NOT NULL COMMENT '结束周次',
    week_type TINYINT DEFAULT 0 COMMENT '0-每周 1-单周 2-双周',
    is_borrowed TINYINT DEFAULT 0 COMMENT '是否借用：0-否 1-是',
    borrow_reason VARCHAR(500) COMMENT '借用原因',
    INDEX idx_room_id (room_id),
    INDEX idx_day_period (day_of_week, period_start, period_end),
    INDEX idx_week (week_start, week_end),
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
