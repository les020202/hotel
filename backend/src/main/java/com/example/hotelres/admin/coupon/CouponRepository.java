package com.example.hotelres.admin.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    Optional<Coupon> findByCode(String code);

    // validOnly=true 이면 "오늘 사용 가능한 쿠폰"만
    @Query("""
        SELECT c FROM Coupon c
        WHERE (:validOnly = false)
           OR ((c.validFrom IS NULL OR c.validFrom <= CURRENT_DATE)
           AND  (c.validTo   IS NULL OR c.validTo   >= CURRENT_DATE))
        ORDER BY c.createdAt DESC
    """)
    List<Coupon> findForList(@Param("validOnly") boolean validOnly);
}
