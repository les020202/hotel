// src/main/java/com/example/hotelres/review/ReviewController.java
package com.example.hotelres.review;

import com.example.hotelres.review.dto.ReviewDtos.EligibilityResponse;
import com.example.hotelres.review.dto.ReviewDtos.ListResponse;
import com.example.hotelres.review.dto.ReviewDtos.RatingResponse;
import com.example.hotelres.review.dto.ReviewDtos.ReportRequest; // ✅ 이 DTO를 사용

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;                       // [NEW]
import org.jsoup.safety.Safelist;           // [NEW]
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;
    private final com.example.hotelres.user.UserRepository userRepository;

    /** 호텔 상세에서 리뷰 목록(프로필/룸타입 포함), 평균/건수도 함께 내려줌 */
    @GetMapping("/hotels/{hotelId}/reviews")
    public ListResponse list(@PathVariable Long hotelId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size) {
        return reviewService.listByHotel(hotelId, page, size);
    }

    /** 평균/건수 */
    @GetMapping("/hotels/{hotelId}/reviews/rating")
    public RatingResponse rating(@PathVariable Long hotelId) {
        return reviewService.getRating(hotelId);
    }

    /** 현재 사용자 작성 자격 확인 */
    @GetMapping("/hotels/{hotelId}/reviews/eligibility")
    @PreAuthorize("isAuthenticated()")
    public EligibilityResponse eligibility(@PathVariable Long hotelId,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));
        return reviewService.checkEligibility(hotelId, userId);
    }

    /** 리뷰 작성 (사진 1장 포함 가능) */
    @PostMapping(
            path = "/hotels/{hotelId}/reviews",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public com.example.hotelres.review.dto.ReviewDtos.ReviewItem create(@PathVariable Long hotelId,
                                                                        @RequestParam("bookingId") Long bookingId,
                                                                        @RequestParam("rating") Short rating,
                                                                        @RequestParam(value = "comment", required = false) String comment,
                                                                        @RequestParam(value = "photo", required = false) MultipartFile photo,
                                                                        @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));

        // [NEW] XSS 방어: 서버 측 sanitize (허용 태그 제한)
        // 제목 필드가 없고 comment만 받는 구조로 보이므로 comment만 정리
        String cleanedComment = comment == null
                ? null
                : Jsoup.clean(comment, Safelist.basic()); // 필요시 허용태그 커스터마이즈

        // [NOTE] 사진 파일 검사는 ReviewFileStorageService에서 수행(확장자/매직바이트/크기)
        return reviewService.create(hotelId, bookingId, userId, rating, cleanedComment, photo);
    }

    /** 리뷰 신고 (USER/OWNER 구분은 서비스 인자 isOwner=false로 처리) */
    @PostMapping("/reviews/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 성공 시 204
    public void report(@PathVariable Long id,
                       @RequestBody(required = false) ReportRequest req,
                       @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {

        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));

        // [NEW] 신고 사유/상세도 sanitize
        String reason = (req == null || req.getReason() == null)
                ? null
                : Jsoup.clean(req.getReason(), Safelist.none()); // 텍스트만
        String detail = (req == null || req.getDetail() == null)
                ? null
                : Jsoup.clean(req.getDetail(), Safelist.basic()); // 필요시 태그 제한

        reviewService.report(id, userId, reason, detail, false);
    }

    /** 🔹 내 리뷰 목록 (숨김 포함) */
    @GetMapping("/my/reviews")
    @PreAuthorize("isAuthenticated()")
    public ListResponse listMine(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));
        return reviewService.listMine(userId, page, size);
    }

    /** 🔹 내 리뷰 삭제 (신규 경로) */
    @DeleteMapping("/my/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteMineByMyPath(@PathVariable Long id,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));
        reviewService.deleteMine(id, userId);
    }

    /** (호환용) 기존 경로 유지 */
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMineCompat(@PathVariable Long id,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));
        reviewService.deleteMine(id, userId);
    }
}
