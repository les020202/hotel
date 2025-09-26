package com.example.hotelres.user.coupon;

import com.example.hotelres.reservation.CouponIssuance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCouponIssuanceRepository extends JpaRepository<CouponIssuance, Long> {

    // LAZY 문제 방지: 쿠폰을 한 번에 끌고 오기
    @Query("""
           select ci
           from CouponIssuance ci
           join fetch ci.coupon c
           where ci.userId = :userId
           order by ci.issuedAt desc
           """)
    List<CouponIssuance> findAllForUserWithCoupon(@Param("userId") Long userId);

    boolean existsByUserIdAndCoupon_Id(Long userId, Long couponId);
}
