// src/main/java/com/example/hotelres/owner/OwnerRoomsController.java
package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/rooms")
public class OwnerRoomsController {

    private final OwnerRoomsService svc;

    public OwnerRoomsController(OwnerRoomsService svc) {
        this.svc = svc;
    }

    @GetMapping("/status")
    public List<RoomStatusDto> status(@PathVariable Long hotelId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return svc.getStatus(hotelId, date);
    }

    public static record HkReq(String housekeepingStatus) {}
    public static record StatusReq(String status) {}

    @PatchMapping("/{roomId}/housekeeping")
    public void updateHk(@PathVariable Long hotelId,
                         @PathVariable Long roomId,
                         @RequestBody HkReq req) {
        svc.updateHk(hotelId, roomId, HousekeepingStatus.valueOf(req.housekeepingStatus()));
    }

    // ✅ date를 쿼리파라미터로 받아서 서비스로 전달
    @PatchMapping("/{roomId}/status")
    public void updateStatus(@PathVariable Long hotelId,
                             @PathVariable Long roomId,
                             @RequestBody StatusReq req,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            svc.updateStatus(hotelId, roomId, RoomStatus.valueOf(req.status()), date);
        } catch (IllegalArgumentException e) {
            // 예: room mismatch, 잘못된 enum 등 → 400/404로 내려서 프론트가 이유를 알 수 있게
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
