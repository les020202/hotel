// src/main/java/com/example/hotelres/review/ReviewAdminController.java
package com.example.hotelres.review;

import com.example.hotelres.review.dto.AdminReviewDtos.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class ReviewAdminController {

    private final ReviewAdminService adminService; // ★ 관리자 전용 서비스
    private final ReviewService reviewService;     // 삭제/숨김 등 공용 로직 재사용

    /**
     * 관리자 리스트(호텔명/작성자 포함, 신고 집계 포함)
     * - from/to는 '날짜'만 받고, 서버에서 [from 00:00, to+1 00:00)로 변환
     * - reportedOnly는 선택(신고가 1건 이상인 리뷰만)
     */
    @GetMapping
    public Page<Row> list(@RequestParam(required = false) Long hotelId,
                          @RequestParam(required = false) Boolean visible,
                          @RequestParam(required = false) Boolean reportedOnly,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {

        LocalDateTime fromAt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toAt   = (to   == null) ? null : to.plusDays(1).atStartOfDay(); // [from, to+1) 범위

        return adminService.list(
                hotelId,
                visible,
                reportedOnly,
                fromAt,
                toAt,
                (q == null || q.isBlank()) ? null : q,
                PageRequest.of(page, size)
        );
    }

    /** 관리자 강제 삭제(신고/사진 포함 완전 삭제 + 호텔 평점 재계산) */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reviewService.deleteByAdmin(id);
    }
}
