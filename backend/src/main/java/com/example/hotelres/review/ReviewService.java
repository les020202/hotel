// src/main/java/com/example/hotelres/review/ReviewService.java
package com.example.hotelres.review;

import com.example.hotelres.common.storage.ReviewFileStorageService;
import com.example.hotelres.owner.BookingEntity;
import com.example.hotelres.owner.BookingRepository;
import com.example.hotelres.payment.PaymentRepository;
import com.example.hotelres.payment.PaymentStatus;
import com.example.hotelres.review.dto.ReviewDtos;
import com.example.hotelres.review.dto.ReviewDtos.ListResponse;
import com.example.hotelres.review.dto.ReviewDtos.RatingResponse;
import com.example.hotelres.review.dto.ReviewDtos.ReviewItem;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewFileStorageService reviewFileStorageService;
    private final UserRepository userRepository;
    private final ReviewReportRepository reviewReportRepository;

    @PersistenceContext
    private EntityManager em;

    // ───────────────────────── helpers ─────────────────────────
    private static Double round1(Double v) {
        if (v == null) return null;
        return Math.round(v * 10.0) / 10.0;
    }

    @Transactional
    protected void recomputeAndUpdateHotelRating(Long hotelId) {
        Double avg = reviewRepository.avgRating(hotelId);
        Double rounded = round1(avg);
        em.createNativeQuery("UPDATE hotels SET rating = :r WHERE id = :id")
                .setParameter("r", rounded)
                .setParameter("id", hotelId)
                .executeUpdate();
    }

    private void deleteAllPhotosPhysicalAndRows(Long reviewId) {
        var photos = reviewPhotoRepository.findByReviewId(reviewId);
        for (ReviewPhoto p : photos) {
            var url = p.getUrl();
            if (url != null && !url.isBlank()) {
                try { reviewFileStorageService.deleteByUrl(url); } catch (Exception ignore) {}
            }
        }
        reviewPhotoRepository.deleteAll(photos);
    }

    // ───────────────────────── queries ─────────────────────────
    @Transactional(readOnly = true)
    public ListResponse listByHotel(Long hotelId, int page, int size) {
        Page<Review> p = reviewRepository.findVisibleByHotel(hotelId, PageRequest.of(page, size));
        List<ReviewItem> items = p.getContent().stream().map(this::toItemWithPhotosAndMeta).toList();
        Double avg = reviewRepository.avgRating(hotelId);
        long cnt = reviewRepository.countVisible(hotelId);
        return ListResponse.builder()
                .content(items)
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .number(p.getNumber())
                .size(p.getSize())
                .avgRating(avg == null ? 0d : avg)
                .count(cnt)
                .build();
    }

    @Transactional(readOnly = true)
    public RatingResponse getRating(Long hotelId) {
        Double avg = reviewRepository.avgRating(hotelId);
        long cnt = reviewRepository.countVisible(hotelId);
        return new RatingResponse(avg == null ? 0.0 : avg, cnt);
    }

    @Transactional(readOnly = true)
    public ReviewDtos.EligibilityResponse checkEligibility(Long hotelId, Long userId) {
        List<BookingEntity> candidates =
                bookingRepository.findPastBookingsForReview(userId, hotelId, LocalDate.now());
        if (candidates.isEmpty()) {
            return ReviewDtos.EligibilityResponse.builder().eligible(false).reason("투숙 완료된 예약이 없습니다.").build();
        }
        for (BookingEntity b : candidates) {
            boolean paid = paymentRepository.existsByBookingIdAndStatus(b.getId(), PaymentStatus.SUCCEEDED);
            if (!paid) continue;
            boolean already = reviewRepository.existsByUserIdAndBookingId(userId, b.getId());
            if (already) continue;
            return ReviewDtos.EligibilityResponse.builder().eligible(true).bookingId(b.getId()).build();
        }
        return ReviewDtos.EligibilityResponse.builder()
                .eligible(false).reason("작성 가능한 예약(결제성공/미리뷰)이 없습니다.").build();
    }

    // ───────────────────────── commands ─────────────────────────
    @Transactional
    public ReviewItem create(Long hotelId, Long bookingId, Long userId,
                             short rating, String comment, MultipartFile photo) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");

        var ok = checkEligibility(hotelId, userId);
        if (!ok.isEligible() || (ok.getBookingId() != null && !ok.getBookingId().equals(bookingId))) {
            throw new IllegalStateException("리뷰 작성 권한이 없습니다.");
        }

        Review saved = reviewRepository.save(
                Review.builder()
                        .hotelId(hotelId)
                        .bookingId(bookingId)
                        .userId(userId)
                        .rating(rating)
                        .comment(comment)
                        .visible(true)
                        .build()
        );

        if (photo != null && !photo.isEmpty()) {
            String url = reviewFileStorageService.save(photo);
            reviewPhotoRepository.save(ReviewPhoto.builder().reviewId(saved.getId()).url(url).build());
        }

        // 리뷰 생성 후 호텔 평점 갱신(visible=true만 반영)
        recomputeAndUpdateHotelRating(hotelId);

        return toItemWithPhotosAndMeta(saved);
    }

    /* 신고: 접수만 (visible 변경 없음) */
    @Transactional
    public void report(Long reviewId, Long reporterUserId, String reason) {
        report(reviewId, reporterUserId, reason, null, false);
    }

    @Transactional
    public void report(Long reviewId, Long reporterUserId, String reason, String detail, boolean isOwner) {
        // 1) 리뷰/사용자 존재 확인 (FK 실패를 사전에 잡기)
        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));
        if (!userRepository.existsById(reporterUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고자 계정이 존재하지 않습니다.");
        }

        // 2) 값 정규화
        final String role = isOwner ? "OWNER" : "USER";
        final String rsn  = (reason == null || reason.isBlank()) ? "기타" : reason;
        final String det  = (detail == null || detail.isBlank()) ? null : detail;

        // 3) 중복이면 바로 반환(멱등)
        if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterUserId)) return;

        // 4) INSERT IGNORE 시도
        int inserted = reviewReportRepository.insertIgnore(reviewId, reporterUserId, role, rsn, det);

        // 5) 삽입 결과가 0행이면 두 가지 가능성:
        //    - (경합으로) 그 사이에 중복이 생김 → OK (멱등)
        //    - 무결성 위반 등으로 실제로 저장 실패 → 에러로 알려주기
        if (inserted == 0) {
            if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterUserId)) {
                return; // 경합으로 이미 들어간 경우
            }
            // 여기로 오면 IGNORE가 무시한 오류(예: FK, 길이초과) 가능성이 큼
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 저장 실패(무결성 위반). 입력값/계정/길이를 확인하세요.");
        }
    }

    /* 사용자 본인 삭제 */
    @Transactional
    public void deleteMine(Long reviewId, Long userId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));
        if (!r.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 리뷰만 삭제할 수 있습니다.");
        }
        deleteAllPhotosPhysicalAndRows(r.getId());
        reviewRepository.delete(r);
        recomputeAndUpdateHotelRating(r.getHotelId());
    }

    /* ───────────── 관리자/오너 운영용 유틸 ───────────── */

    /** 관리자가 숨김 처리(HIDE) 결정 시 호출 */
    @Transactional
    public void adminHideReview(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));
        if (Boolean.TRUE.equals(r.getVisible())) {
            r.setVisible(false);
            reviewRepository.save(r);
            recomputeAndUpdateHotelRating(r.getHotelId()); // 평균 재계산
        }
    }

    /** 관리자 강제 삭제(사진 포함 완전 삭제) */
    @Transactional
    public void adminHardDeleteReview(Long reviewId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));
        deleteAllPhotosPhysicalAndRows(r.getId());
        reviewRepository.delete(r);
        recomputeAndUpdateHotelRating(r.getHotelId());
    }

    /** 컨트롤러에서 호출하는 관리자 삭제 단일 엔트리 (신고도 함께 정리) */
    @Transactional
    public void deleteByAdmin(Long reviewId) {
        // 신고 먼저 정리(외래키 제약/잔존데이터 방지)
        reviewReportRepository.deleteByReviewId(reviewId);
        // 실제 리뷰/사진 삭제 + 평점 갱신
        adminHardDeleteReview(reviewId);
    }

    // ───────────────────────── mapper ─────────────────────────
    @Transactional(readOnly = true)
    protected ReviewItem toItemWithPhotosAndMeta(Review r) {
        // 사진들
        List<String> photos = reviewPhotoRepository.findByReviewId(r.getId())
                .stream().map(ReviewPhoto::getUrl).toList();

        // 예약(체크인/아웃) — bookingId가 null일 수 있으니 안전하게
        Optional<BookingEntity> bookingOpt = Optional.empty();
        if (r.getBookingId() != null) {
            bookingOpt = bookingRepository.findById(r.getBookingId());
        }

        // 룸타입명 — bookingId null이면 조회하지 않음
        String roomTypeName = null;
        if (r.getBookingId() != null) {
            roomTypeName = bookingRepository.findRoomTypeNameByBookingId(r.getBookingId());
        }

        // 작성자
        Optional<User> userOpt = userRepository.findById(r.getUserId());
        String reviewerName = userOpt.map(User::getName).orElse("익명");
        String profileImageType = userOpt
                .map(u -> u.getProfileImageType() == null ? "NONE" : u.getProfileImageType().name())
                .orElse("NONE");
        String profileImageUrl = userOpt.map(User::getProfileImageUrl).orElse(null);
        String profileImageTemplate = userOpt
                .map(u -> u.getProfileImageTemplate() == null ? null : u.getProfileImageTemplate().name())
                .orElse(null);

        return ReviewItem.builder()
                .id(r.getId())
                .hotelId(r.getHotelId())
                .bookingId(r.getBookingId())
                .userId(r.getUserId())
                .rating(r.getRating())
                .comment(r.getComment())
                .visible(Boolean.TRUE.equals(r.getVisible()))
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .photos(photos)
                .reviewerName(reviewerName)
                .profileImageType(profileImageType)
                .profileImageUrl(profileImageUrl)
                .profileImageTemplate(profileImageTemplate)
                .roomTypeName(roomTypeName)
                .checkIn(bookingOpt.map(BookingEntity::getCheckIn).orElse(null))
                .checkOut(bookingOpt.map(BookingEntity::getCheckOut).orElse(null))
                .build();
    }
}
