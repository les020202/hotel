package com.example.hotelres.coupon;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

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
