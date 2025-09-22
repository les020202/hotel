// path: src/main/java/com/example/hotelres/reservation/CouponRepository.java
package com.example.hotelres.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
}
