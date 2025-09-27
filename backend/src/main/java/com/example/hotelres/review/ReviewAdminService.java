// src/main/java/com/example/hotelres/review/ReviewAdminService.java
package com.example.hotelres.review;

import com.example.hotelres.review.dto.AdminReviewDtos.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewAdminService {

    private final ReviewRepository reviewRepository;

    /** 관리자 리스트(호텔명/작성자 포함, 날짜=일자만) */
    @Transactional(readOnly = true)
    public Page<Row> list(
            Long hotelId,
            Boolean visible,
            Boolean reportedOnly,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            String q,
            Pageable pageable
    ) {
        Page<ReviewRepository.ReviewManageRow> page =
                reviewRepository.searchManageWithReports(hotelId, visible, reportedOnly, fromAt, toAt, q, pageable);

        return page.map(this::mapRow);
    }

    private Row mapRow(ReviewRepository.ReviewManageRow r) {
        LocalDateTime created = r.getCreated_at();
        LocalDate createdDate = (created != null) ? created.toLocalDate() : null;

        return Row.builder()
                .id(r.getId())
                .hotelId(r.getHotel_id())
                .userId(r.getUser_id())
                .hotelName(r.getHotel_name())   // ★ 여기서 세팅
                .userName(r.getUser_name())     // ★ 여기서 세팅
                .visible(r.getVisible())
                .createdDate(createdDate)       // YYYY-MM-DD만
                .rating(r.getRating())
                .comment(r.getComment())
                .reportCount(nz(r.getReport_count()))
                .userReportCount(nz(r.getUser_report_count()))
                .ownerReportCount(nz(r.getOwner_report_count()))
                .latestReportAt(r.getLatest_report_at())
                .build();
    }

    private int nz(Integer v) { return v == null ? 0 : v; }
}
