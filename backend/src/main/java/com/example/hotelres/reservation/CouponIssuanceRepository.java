// path: backend/src/main/java/com/example/hotelres/reservation/CouponIssuanceRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponIssuanceRepository extends JpaRepository<CouponIssuance, Long> {

    /** 특정 유저가 지금 사용할 수 있는 발급 1건 (코드 일치) */
    @Query("""
        select ci
        from CouponIssuance ci
          join fetch ci.coupon c
        where ci.userId = :userId
          and c.code = :code
          and ci.status = com.example.hotelres.reservation.CouponIssuanceStatus.AVAILABLE
          and (ci.expiresAt is null or ci.expiresAt > :now)
          and ( (c.validFrom is null or c.validFrom <= :today)
            and (c.validTo   is null or c.validTo   >= :today) )
        """)
    Optional<CouponIssuance> findAvailableByUserAndCode(@Param("userId") Long userId,
                                                        @Param("code") String code,
                                                        @Param("today") LocalDate today,
                                                        @Param("now") LocalDateTime now);

    /** 특정 유저의 현재 시점 사용 가능 전체 목록 */
    @Query("""
        select ci
        from CouponIssuance ci
          join fetch ci.coupon c
        where ci.userId = :userId
          and ci.status = com.example.hotelres.reservation.CouponIssuanceStatus.AVAILABLE
          and (ci.expiresAt is null or ci.expiresAt > :now)
          and ( (c.validFrom is null or c.validFrom <= :today)
            and (c.validTo   is null or c.validTo   >= :today) )
        order by ci.expiresAt nulls last
        """)
    List<CouponIssuance> findAvailableForUser(@Param("userId") Long userId,
                                              @Param("today") LocalDate today,
                                              @Param("now") LocalDateTime now);
}
