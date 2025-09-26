// path: backend/src/main/java/com/example/hotelres/user/coupon/UserCouponService.java
package com.example.hotelres.user.coupon;

import com.example.hotelres.admin.coupon.Coupon;
import com.example.hotelres.config.AppCouponProps;
import com.example.hotelres.reservation.CouponIssuance;
import com.example.hotelres.reservation.CouponIssuanceStatus;
import com.example.hotelres.user.coupon.dto.UserCouponDto;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final UserCouponRepository couponRepository;              // coupons
    private final UserCouponIssuanceRepository issuanceRepository;    // coupon_issuance
    private final AppCouponProps props;                               // .env 바인딩 (welcomeCode, welcomeExpDays)

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 내 쿠폰 조회
     * @param userId 사용자 ID
     * @param all true면 전체, false면 오늘 기준 유효 + AVAILABLE 만
     */
    @Transactional(readOnly = true)
    public List<UserCouponDto> getMyCoupons(Long userId, boolean all) {
        // fetch-join으로 쿠폰까지 한 번에 로드 (N+1 방지)
        List<CouponIssuance> rows = issuanceRepository.findAllForUserWithCoupon(userId);

        LocalDate today = LocalDate.now();

        return rows.stream()
                .filter(ci -> {
                    if (all) return true;
                    // 유효기간(쿠폰 정의) + 상태 AVAILABLE
                    Coupon c = ci.getCoupon();
                    LocalDate from = c.getValidFrom();
                    LocalDate to   = c.getValidTo();
                    boolean fromOk = (from == null) || !from.isAfter(today);
                    boolean toOk   = (to == null)   || !to.isBefore(today);
                    return fromOk && toOk && ci.getStatus() == CouponIssuanceStatus.AVAILABLE;
                })
                .sorted(Comparator.comparing(CouponIssuance::getIssuedAt).reversed())
                .map(this::toDto)
                .toList();
    }

    /** 엔티티 → DTO(record) 매핑 */
    private UserCouponDto toDto(CouponIssuance ci) {
        Coupon c = ci.getCoupon();
        String status    = ci.getStatus() == null ? null : ci.getStatus().name();
        String issuedAt  = ci.getIssuedAt() == null ? null : ci.getIssuedAt().toLocalDate().format(D);
        String validFrom = c.getValidFrom() == null ? null : c.getValidFrom().format(D);
        String validTo   = c.getValidTo()   == null ? null : c.getValidTo().format(D);

        return new UserCouponDto(
                ci.getId(),
                c.getCode(),
                c.getTitle(),
                c.getAmount(),
                c.isStackable(),
                status,
                issuedAt,
                validFrom,
                validTo
        );
    }

    /**
     * 환영 쿠폰 자동지급
     * - 회원가입 트랜잭션 오염 방지를 위해 신규 트랜잭션으로 분리(REQUIRES_NEW)
     * - 실패(중복/제약 등)는 false로만 반환, 예외 전파하지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean grantWelcomeCouponIfNeeded(Long userId) {
    // props가 혹시라도 null이면 기본값으로 폴백
    String welcomeCode = (props.welcomeCode() == null || props.welcomeCode().isBlank())
            ? "WELCOME10K" : props.welcomeCode();
    int welcomeExpDays = props.welcomeExpDays();

    System.out.println("[WELCOME] try userId=" + userId
            + " code=" + welcomeCode + " expDays=" + welcomeExpDays);

    var couponOpt = couponRepository.findByCode(welcomeCode);
    if (couponOpt.isEmpty()) {
        System.out.println("[WELCOME] coupon not found for code=" + welcomeCode);
        return false;
    }
        Coupon coupon = couponOpt.get();

        if (issuanceRepository.existsByUserIdAndCoupon_Id(userId, coupon.getId())) {
            System.out.println("[WELCOME] already issued userId=" + userId);
            return false;
        }

        CouponIssuance ci = new CouponIssuance();
        ci.setCoupon(coupon);
        ci.setUserId(userId);
        ci.setStatus(CouponIssuanceStatus.AVAILABLE);
        if (welcomeExpDays > 0) {
            ci.setExpiresAt(LocalDateTime.now().plusDays(welcomeExpDays));
        }

        try {
            issuanceRepository.saveAndFlush(ci);
            System.out.println("[WELCOME] issued OK id=" + ci.getId());
            return true;
        } catch (DataIntegrityViolationException | PersistenceException e) {
            e.printStackTrace(); // 디버깅 편의용
            return false;
        }
    }
}
