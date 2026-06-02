package com.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
//     * 查询教室在指定时间的课程
     */
    @Select("SELECT * FROM schedule WHERE room_id = #{roomId} AND day_of_week = #{dayOfWeek} " +
            "AND period_start <= #{period} AND period_end >= #{period} " +
            "AND week_start <= #{week} AND week_end >= #{week}")
    List<Schedule> findByRoomAndTime(@Param("roomId") Long roomId,
                                     @Param("dayOfWeek") Integer dayOfWeek,
                                     @Param("period") Integer period,
                                     @Param("week") Integer week);

    /**
     * 查询教室在指定周次的所有课程
     */
    @Select("SELECT * FROM schedule WHERE room_id = #{roomId} AND week_start <= #{week} AND week_end >= #{week}")
    List<Schedule> findByRoomAndWeek(@Param("roomId") Long roomId, @Param("week") Integer week);
}
