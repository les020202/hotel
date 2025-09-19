// src/main/java/.../coupon/Coupon.java
package com.example.hotelres.admin.coupon;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name="coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, unique=true, length=50)
  private String code;

  @Column(nullable=false, length=100)
  private String title;        // 표시용 이름

  @Column(nullable=false)
  private Integer amount;      // 원 단위 고정 차감

  @Column(nullable=false)
  private Boolean stackable = false;

  @Column(name="valid_from")
  private LocalDate validFrom; // DATE

  @Column(name="valid_to")
  private LocalDate validTo;   // DATE

  @CreationTimestamp                                // ★ 추가
  @Column(name="created_at", updatable=false)       // ★ insertable=false 제거!
  private LocalDateTime createdAt;
}
