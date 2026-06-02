package com.classroom.controller;

import com.classroom.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 获取所有教室及当前状态
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRoomsWithStatus());
    }

    /**
     * 搜索教室
     */
    @GetMapping("/rooms/search")
    public ResponseEntity<List<Map<String, Object>>> searchRooms(@RequestParam String keyword) {
        return ResponseEntity.ok(roomService.searchRooms(keyword));
    }

    /**
     * 获取教室详情及课表
     */
    @GetMapping("/rooms/{id}")
    public ResponseEntity<Map<String, Object>> getRoomDetail(@PathVariable Long id) {
        Map<String, Object> detail = roomService.getRoomDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 查询教室指定时间状态
     */
    @GetMapping("/rooms/{id}/status")
    public ResponseEntity<Map<String, Object>> getRoomStatus(
            @PathVariable Long id,
            @RequestParam Integer week,
            @RequestParam Integer day,
            @RequestParam Integer period) {
        Map<String, Object> status = roomService.getRoomStatus(id, week, day, period);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    /**
     * 获取当前时间信息
     */
    @GetMapping("/time")
    public ResponseEntity<Map<String, Object>> getCurrentTime() {
        return ResponseEntity.ok(roomService.getCurrentTimeInfo());
    }
}
