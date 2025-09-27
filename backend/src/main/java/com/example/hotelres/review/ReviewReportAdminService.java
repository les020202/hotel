// src/main/java/com/example/hotelres/review/ReviewReportAdminService.java
package com.example.hotelres.review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewReportAdminService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void resolve(Long reportId,
                        Long adminUserId,
                        ReviewReport.Action action,
                        String adminComment) {
        var rr = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고내역을 찾을 수 없습니다."));

        if (rr.getStatus() == ReviewReport.Status.RESOLVED) return;

        rr.setStatus(ReviewReport.Status.RESOLVED);
        rr.setAction(action);
        rr.setAdminComment(adminComment);
        rr.setHandledByAdminId(adminUserId);
        rr.setHandledAt(LocalDateTime.now());
        reviewReportRepository.save(rr);

        // HIDE 선택 시 실제 리뷰 숨김
        if (action == ReviewReport.Action.HIDE) {
            var review = reviewRepository.findById(rr.getReviewId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));
            review.setVisible(false);
            reviewRepository.save(review);
        }
    }
}
