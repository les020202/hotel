// src/main/java/com/example/hotelres/review/ReviewService.java
package com.example.hotelres.review;

import com.example.hotelres.common.storage.ReviewFileStorageService;
import com.example.hotelres.owner.BookingEntity;
import com.example.hotelres.owner.BookingRepository;
import com.example.hotelres.payment.PaymentRepository;
import com.example.hotelres.payment.PaymentStatus;
import com.example.hotelres.review.dto.ReviewDtos.*;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;              // ★ 추가
import jakarta.persistence.PersistenceContext;       // ★ 추가
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

    @PersistenceContext
    private EntityManager em;                        // ★ 추가

    // 소수 1자리 반올림
    private static Double round1(Double v) {
        if (v == null) return null;
        return Math.round(v * 10.0) / 10.0;
    }

    // hotels.rating 갱신 (visible=true 평균 사용)
    @Transactional
    protected void recomputeAndUpdateHotelRating(Long hotelId) {   // ★ 추가
        Double avg = reviewRepository.avgRating(hotelId);          // NULL이면 0으로 보관할지, NULL 유지할지 정책 선택
        Double rounded = round1(avg);
        em.createNativeQuery("UPDATE hotels SET rating = :r WHERE id = :id")
                .setParameter("r", rounded)
                .setParameter("id", hotelId)
                .executeUpdate();
    }

    /** 호텔 상세에서 리뷰 목록 (가시성 true만) */
    @Transactional(readOnly = true)
    public ListResponse listByHotel(Long hotelId, int page, int size) {
        Page<Review> p = reviewRepository.findVisibleByHotel(hotelId, PageRequest.of(page, size));

        List<ReviewItem> items = p.getContent().stream()
                .map(this::toItemWithPhotosAndMeta)
                .toList();

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

    /** 평균/건수 요약 */
    @Transactional(readOnly = true)
    public RatingResponse getRating(Long hotelId) {
        Double avg = reviewRepository.avgRating(hotelId);
        long cnt = reviewRepository.countVisible(hotelId);
        return new RatingResponse(avg == null ? 0.0 : avg, cnt);
    }

    /** 작성 자격 확인 */
    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(Long hotelId, Long userId) {
        List<BookingEntity> candidates =
                bookingRepository.findPastBookingsForReview(userId, hotelId, LocalDate.now());

        if (candidates.isEmpty()) {
            return EligibilityResponse.builder()
                    .eligible(false)
                    .reason("투숙 완료된 예약이 없습니다.")
                    .build();
        }

        for (BookingEntity b : candidates) {
            boolean paid = paymentRepository.existsByBookingIdAndStatus(b.getId(), PaymentStatus.SUCCEEDED);
            if (!paid) continue;

            boolean already = reviewRepository.existsByUserIdAndBookingId(userId, b.getId());
            if (already) continue;

            return EligibilityResponse.builder()
                    .eligible(true)
                    .bookingId(b.getId())
                    .build();
        }

        return EligibilityResponse.builder()
                .eligible(false)
                .reason("작성 가능한 예약(결제성공/미리뷰)이 없습니다.")
                .build();
    }

    /** 리뷰 작성(사진 1장 허용) */
    @Transactional
    public ReviewItem create(Long hotelId,
                             Long bookingId,
                             Long userId,
                             short rating,
                             String comment,
                             MultipartFile photo) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }

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
            reviewPhotoRepository.save(
                    ReviewPhoto.builder()
                            .reviewId(saved.getId())
                            .url(url)
                            .build()
            );
        }

        // ★ 호텔 평점 갱신
        recomputeAndUpdateHotelRating(hotelId);

        return toItemWithPhotosAndMeta(saved);
    }

    /** 신고 → 블라인드 */
    @Transactional
    public void report(Long reviewId, Long reporterUserId, String reason) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));
        r.setVisible(false);
        reviewRepository.save(r);

        // ★ 호텔 평점 갱신
        recomputeAndUpdateHotelRating(r.getHotelId());
    }

    // ===== mapper =====
    @Transactional(readOnly = true)
    protected ReviewItem toItemWithPhotosAndMeta(Review r) {
        // 사진들
        List<String> photos = reviewPhotoRepository.findByReviewId(r.getId())
                .stream().map(ReviewPhoto::getUrl).toList();

        // 예약(체크인/아웃)
        Optional<BookingEntity> bookingOpt = bookingRepository.findById(r.getBookingId());

        // 룸타입명
        String roomTypeName = bookingRepository.findRoomTypeNameByBookingId(r.getBookingId());

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

    @Transactional
    public void deleteMine(Long reviewId, Long userId) {
        Review r = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 존재하지 않습니다."));

        if (!r.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 리뷰만 삭제할 수 있습니다.");
        }

        // 사진 파일/레코드 정리
        var photos = reviewPhotoRepository.findByReviewId(r.getId());
        for (ReviewPhoto p : photos) {
            var url = p.getUrl();
            if (url != null && !url.isBlank()) {
                try { reviewFileStorageService.deleteByUrl(url); } catch (Exception ignore) {}
            }
        }
        reviewPhotoRepository.deleteAll(photos);

        // 리뷰 하드 삭제
        reviewRepository.delete(r);

        // ★ 호텔 평점 갱신
        recomputeAndUpdateHotelRating(r.getHotelId());
    }
}
