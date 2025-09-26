// src/main/java/com/example/hotelres/admin/hotelapp/HotelApplicationAuditController.java
package com.example.hotelres.admin.hotelapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hotelapp")
public class HotelApplicationAuditController {

    private final HotelApplicationAuditService svc;

    public HotelApplicationAuditController(HotelApplicationAuditService svc) {
        this.svc = svc;
    }

    /** 목록 (간단 버전) ?status=&q= */
    @GetMapping
    public List<HotelApplicationAuditService.HotelAppRow> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        return svc.search(status, q);
    }

    /** 상세 */
    @GetMapping("/{id}")
    public HotelApplicationAuditService.HotelAppRow get(@PathVariable long id) {
        return svc.getOne(id);
    }

    /** 승인 → 프로시저 호출 → 생성/연결된 hotelId 반환 */
    @PostMapping("/{id}/approve")
    public ResponseEntity<HotelApplicationAuditService.ApproveResponse> approve(
            @PathVariable long id,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId // 프론트에서 넣어줌
    ) {
        long aid = adminId != null ? adminId : 0L;
        long hotelId = svc.approve(id, aid);
        return ResponseEntity.ok(new HotelApplicationAuditService.ApproveResponse(hotelId));
    }

    /** 반려(사유 저장) */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable long id,
            @RequestBody HotelApplicationAuditService.RejectRequest body,
            @RequestHeader(value = "X-Admin-Id", required = false) Long adminId
    ) {
        long aid = adminId != null ? adminId : 0L;
        svc.reject(id, aid, body.memo());
        return ResponseEntity.ok().build();
    }
}
