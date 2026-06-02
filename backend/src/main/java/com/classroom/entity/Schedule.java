package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("schedule")
public class Schedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomId;

    private String courseName;

    private String teacher;

    private String classInfo;

    private Integer studentCount;

    private Integer dayOfWeek;

    private Integer periodStart;

    private Integer periodEnd;

    private Integer weekStart;

    private Integer weekEnd;

    /**
     * 0-每周 1-单周 2-双周
     */
    private Integer weekType;

    /**
     * 是否借用：0-否 1-是
     */
    private Integer isBorrowed;

    private String borrowReason;
}
