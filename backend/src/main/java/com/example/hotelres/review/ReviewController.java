// src/main/java/com/example/hotelres/review/ReviewController.java
package com.example.hotelres.review;

import com.example.hotelres.review.dto.ReviewDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
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
                                           @AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("사용자 ID를 찾을 수 없습니다."));
        return reviewService.checkEligibility(hotelId, userId);
    }

    /** 리뷰 작성 (사진 1장 포함 가능) */
    @PostMapping(
            path = "/hotels/{hotelId}/reviews",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ReviewItem create(@PathVariable Long hotelId,
                             @RequestParam("bookingId") Long bookingId,
                             @RequestParam("rating") Short rating,
                             @RequestParam(value = "comment", required = false) String comment,
                             @RequestParam(value = "photo", required = false) MultipartFile photo,
                             @AuthenticationPrincipal(expression = "username") String loginId) {

        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("사용자 ID를 찾을 수 없습니다."));
        return reviewService.create(hotelId, bookingId, userId, rating, comment, photo);
    }

    /** 리뷰 신고 */
    @PostMapping("/reviews/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public void report(@PathVariable Long id,
                       @RequestBody ReportRequest req,
                       @AuthenticationPrincipal User principal) {
        reviewService.report(id, null, req.getReason());
    }

    // 추가
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteMine(@PathVariable Long id,
                           @AuthenticationPrincipal(expression = "username") String loginId) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 ID를 찾을 수 없습니다."));
        reviewService.deleteMine(id, userId);
    }

}
