package com.example.hotelres.user.coupon;

import com.example.hotelres.admin.coupon.Coupon;
import com.example.hotelres.common.ApiException;
import com.example.hotelres.reservation.CouponIssuance;
import com.example.hotelres.reservation.CouponIssuanceStatus;
import com.example.hotelres.user.coupon.dto.ClaimCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 쿠폰 코드 입력 → 현재 로그인 사용자에게 발급(issuance)
 */
@Service
@RequiredArgsConstructor
public class CouponClaimService {

    private final UserCouponRepository couponRepository;              // coupons (마스터)
    private final UserCouponIssuanceRepository issuanceRepository;    // coupon_issuance (발급)

    @Transactional
    public ClaimCouponResponse claim(Long userId, String code) {
        if (userId == null) throw new ApiException("로그인이 필요합니다.");
        if (code == null || code.isBlank()) throw new ApiException("쿠폰 코드를 입력하세요.");

        // 1) 쿠폰 마스터 조회
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ApiException("존재하지 않는 쿠폰 코드입니다."));

        // 2) 이미 'AVAILABLE' 상태로 보유 중인지(중복) 검사
        if (issuanceRepository.existsByUserIdAndCoupon_IdAndStatus(userId, coupon.getId(), CouponIssuanceStatus.AVAILABLE)) {
            throw new ApiException("이미 보유 중인 쿠폰입니다.");
        }

        // (선택) 유효기간 체크: 마스터 기준 (없으면 무제한)
        LocalDate today = LocalDate.now();
        LocalDate vf = coupon.getValidFrom();
        LocalDate vt = coupon.getValidTo();
        if (vf != null && vf.isAfter(today)) throw new ApiException("아직 사용 시작 전인 쿠폰입니다.");
        if (vt != null && vt.isBefore(today)) throw new ApiException("만료된 쿠폰입니다.");

        // 3) 발급 생성 (엔티티에 실제 존재하는 필드만 설정)
        CouponIssuance ci = new CouponIssuance();
        ci.setUserId(userId);
        ci.setCoupon(coupon);
        ci.setStatus(CouponIssuanceStatus.AVAILABLE);
        ci.setIssuedAt(LocalDateTime.now());
        // ※ expiresAt 정책이 있으면 필요 시 여기서 세팅

        CouponIssuance saved = issuanceRepository.save(ci);

        // 4) 응답 DTO (마스터 값에서 뽑아 채움 — 없으면 null 허용)
        String validFrom = (vf != null) ? vf.toString() : null;
        String validTo   = (vt != null) ? vt.toString() : null;
        Boolean stackable = (coupon.getStackable() != null) ? coupon.getStackable() : Boolean.FALSE;

        return new ClaimCouponResponse(
                saved.getId(),
                coupon.getCode(),
                coupon.getTitle(),
                coupon.getAmount(),
                stackable,
                validFrom,
                validTo,
                "AVAILABLE"
        );
    }
}
