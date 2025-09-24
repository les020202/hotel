package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.hotelapp.HotelApplicationEntity;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotelapp")
public class HotelApplicationAuditController {

    private final HotelApplicationAuditService svc;

    public HotelApplicationAuditController(HotelApplicationAuditService svc) {
        this.svc = svc;
    }

    /** 목록 – status=all/전체/null 이면 전체 */
    @GetMapping("/applications")
    public Page<HotelApplicationEntity> list(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return svc.list(status, page, size);
    }

    /** 단건 */
    @GetMapping("/applications/{id}")
    public HotelApplicationEntity get(@PathVariable Long id) {
        return svc.get(id);
    }

    /** 심사중으로 표시 */
    @PostMapping("/applications/{id}/under-review")
    public HotelApplicationEntity underReview(@PathVariable Long id,
                                              @RequestParam Long adminId,
                                              @RequestParam(required = false) String memo) {
        return svc.markUnderReview(id, adminId, memo);
    }

    /** 승인 */
    @PostMapping("/applications/{id}/approve")
    public HotelApplicationEntity approve(@PathVariable Long id,
                                          @RequestParam Long adminId,
                                          @RequestParam(required = false) String memo) {
        return svc.approve(id, adminId, memo);
    }

    /** 반려 */
    @PostMapping("/applications/{id}/reject")
    public HotelApplicationEntity reject(@PathVariable Long id,
                                         @RequestParam Long adminId,
                                         @RequestParam(required = false) String memo) {
        return svc.reject(id, adminId, memo);
    }
}
