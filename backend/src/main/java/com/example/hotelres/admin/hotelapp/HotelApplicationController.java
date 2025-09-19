package com.example.hotelres.admin.hotelapp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.hotelres.admin.hotelapp.HotelApplication.Status;

@RestController
@RequestMapping("/api/admin/hotelapps")
@RequiredArgsConstructor
public class HotelApplicationController {

    private final HotelApplicationService service;

    // 목록: /api/admin/hotelapps?status=PENDING&region=서울&q=리버뷰&page=0&size=20
    @GetMapping
    public PagedResp<AppListItemDto> list(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(status, region, q, page, size);
    }

    @GetMapping("/{id}")
    public AppDetailDto detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveRequest req,
            @AuthenticationPrincipal(expression = "id") Long adminUserId   // 프로젝트에 맞게 변경
    ) {
        service.approve(id, adminUserId, req == null ? null : req.getNote());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody RejectRequest req,
            @AuthenticationPrincipal(expression = "id") Long adminUserId
    ) {
        service.reject(id, adminUserId, req.getReason(), req.getNote());
        return ResponseEntity.ok().build();
    }
}
