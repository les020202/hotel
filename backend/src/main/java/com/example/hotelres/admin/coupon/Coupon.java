package com.example.hotelres.admin.coupon;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Coupon 엔티티
 * - 쿠폰(할인 코드)의 메타데이터를 보관합니다.
 * - DB 테이블: coupons
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키. AUTO_INCREMENT

    @Column(nullable = false, unique = true, length = 50)
    private String code; // 사용자에게 배포되는 쿠폰 코드 (중복 불가)

    @Column(nullable = false, length = 100)
    private String title; // 쿠폰 이름/설명 (예: "신규 가입 5천원 할인")

    @Column(nullable = false)
    private Integer amount; // 정액 할인 금액 (원화 기준)

    @Column(nullable = false)
    private Boolean stackable = false; // 다른 쿠폰과의 중복 사용 가능 여부

    @Column(name = "valid_from")
    private LocalDate validFrom; // 유효 시작일 (NULL = 제한 없음)

    @Column(name = "valid_to")
    private LocalDate validTo; // 유효 종료일 (NULL = 제한 없음)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시각 (DB 자동 기록)
}
