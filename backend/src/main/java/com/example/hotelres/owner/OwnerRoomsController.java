package com.example.hotelres.owner;

import com.example.hotelres.owner.dto.RoomStatusDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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

    // ✓ 요청 DTO들 (컨트롤러 내부에 정의)
    public static record HkReq(String housekeepingStatus) {}
    public static record StatusReq(String status) {}

    @PatchMapping("/{roomId}/housekeeping")
    public void updateHk(@PathVariable Long hotelId,
                         @PathVariable Long roomId,
                         @RequestBody HkReq req) {
        svc.updateHk(hotelId, roomId, HousekeepingStatus.valueOf(req.housekeepingStatus()));
    }

    @PatchMapping("/{roomId}/status")
    public void updateStatus(@PathVariable Long hotelId,
                             @PathVariable Long roomId,
                             @RequestBody StatusReq req) {
        svc.updateStatus(hotelId, roomId, RoomStatus.valueOf(req.status()));
    }
}
