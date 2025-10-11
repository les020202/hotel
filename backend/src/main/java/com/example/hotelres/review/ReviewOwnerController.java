// src/main/java/com/example/hotelres/review/ReviewOwnerController.java
package com.example.hotelres.review;

import com.example.hotelres.owner.HotelOwnerRepository;
import com.example.hotelres.review.dto.ReviewDtos.ReportRequest;
import com.example.hotelres.review.dto.ReviewDtos.ReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels/{hotelId}/reviews")
@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
public class ReviewOwnerController {

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final HotelOwnerRepository hotelOwnerRepository;
    private final com.example.hotelres.user.UserRepository userRepository;

    /**
     * 오너: 내 호텔 리뷰 목록 (공개/숨김 모두 가능, 신고는 따로)
     * 반환은 사용자 화면과 동일한 ReviewItem(작성자/프로필/룸타입/사진수준 메타 포함)
     */
    @GetMapping
    public Page<ReviewItem> listForOwner(@PathVariable Long hotelId,
                                         @RequestParam(defaultValue = "false") boolean all,
                                         @RequestParam(required = false) Boolean visible,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         Authentication auth) {

        // ADMIN은 패스, OWNER는 소유검증
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            String loginId = auth.getName();
            boolean owns = hotelOwnerRepository.existsByHotelIdAndUserLoginId(hotelId, loginId);
            if (!owns) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 호텔의 리뷰만 조회할 수 있습니다.");
        }

        // all=true면 visible 필터 무시(null), 아니면 기본 true (요청값 우선)
        Boolean vis = all ? null : (visible != null ? visible : Boolean.TRUE);

        LocalDateTime fromAt = (from == null) ? null : from.atStartOfDay();
        // to는 날짜만 들어오므로 익일 00:00 직전까지 포함되도록 +1일 00:00
        LocalDateTime toAt   = (to   == null) ? null : to.atTime(LocalTime.MAX).plusSeconds(1);

        var p = reviewRepository.searchForManage(
                hotelId,
                vis,
                fromAt,
                toAt,
                (q == null || q.isBlank()) ? null : q.trim(),
                PageRequest.of(page, size)
        );
        // Review → ReviewItem(작성자/사진/룸타입 메타 채움)
        return p.map(reviewService::toItemWithPhotosAndMeta);
    }

    /**
     * 오너: 리뷰 신고 (OWNER 플래그)
     * URL은 호텔 경로 아래지만, reviewId만으로 처리
     */
    @PostMapping("/{reviewId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportByOwner(@PathVariable Long hotelId,
                              @PathVariable Long reviewId,
                              @RequestBody(required = false) ReportRequest req,
                              Authentication auth) {

        // 소유검증 (ADMIN이면 스킵)
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            String loginId = auth.getName();
            boolean owns = hotelOwnerRepository.existsByHotelIdAndUserLoginId(hotelId, loginId);
            if (!owns) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 호텔의 리뷰만 신고할 수 있습니다.");
        }

        // 로그인 사용자 → userId
        Long userId = userRepository.findIdByLoginId(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));

        String reason = (req == null) ? null : req.getReason();
        String detail = (req == null) ? null : req.getDetail();

        // OWNER 신고로 멱등 저장
        reviewService.report(reviewId, userId, reason, detail, true);
    }
}
