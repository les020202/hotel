// src/main/java/.../coupon/CouponRepository.java
package com.example.hotelres.admin.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
  boolean existsByCode(String code);
  List<Coupon> findAllByOrderByCreatedAtDesc();
}
