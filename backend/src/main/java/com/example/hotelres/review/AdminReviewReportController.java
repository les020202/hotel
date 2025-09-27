// src/main/java/com/example/hotelres/review/AdminReviewReportController.java
package com.example.hotelres.review;

import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewReportController {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewReportAdminService reviewReportAdminService;
    private final UserRepository userRepository;

    // 관리자 신고 목록
    @GetMapping
    public List<ReviewReportRepository.AdminReportRow> listForAdmin(
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "false") boolean ownerOnly,
            @RequestParam(required = false) String q
    ) {
        String key = (q == null || q.isBlank()) ? null : q;
        // ✅ 레포지토리 시그니처와 동일한 순서로 호출
        return reviewReportRepository.findAdminReports(minRating, maxRating, ownerOnly, key);
    }

    public record ResolveRequest(String action, String adminComment) {}

    // 신고 처리
    @PostMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id,
                        @RequestBody ResolveRequest body,
                        @AuthenticationPrincipal(expression = "username") String loginId) {
        Long adminId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("관리자 ID 조회 실패"));
        var act = "HIDE".equalsIgnoreCase(body.action()) ? ReviewReport.Action.HIDE : ReviewReport.Action.KEEP;
        reviewReportAdminService.resolve(id, adminId, act, body.adminComment());
    }
}
