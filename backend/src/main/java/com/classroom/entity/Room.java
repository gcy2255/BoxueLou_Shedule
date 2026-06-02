package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomName;

    private Integer seatCount;

    private Integer floorNum;

    private String roomType;
}
