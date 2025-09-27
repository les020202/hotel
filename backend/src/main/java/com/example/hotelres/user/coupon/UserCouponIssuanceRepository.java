package com.example.hotelres.user.coupon;

import com.example.hotelres.reservation.CouponIssuance;
import com.example.hotelres.reservation.CouponIssuanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCouponIssuanceRepository extends JpaRepository<CouponIssuance, Long> {

    @Query("""
           select ci
           from CouponIssuance ci
           join fetch ci.coupon c
           where ci.userId = :userId
           order by ci.issuedAt desc
           """)
    List<CouponIssuance> findAllForUserWithCoupon(@Param("userId") Long userId);

    boolean existsByUserIdAndCoupon_Id(Long userId, Long couponId);

    // ✅ 이미 '사용 가능' 상태로 같은 쿠폰을 보유 중인지 빠르게 체크
    boolean existsByUserIdAndCoupon_IdAndStatus(Long userId, Long couponId, CouponIssuanceStatus status);
}
