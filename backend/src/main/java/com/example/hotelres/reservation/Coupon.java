// path: backend/src/main/java/com/example/hotelres/reservation/Coupon.java
package com.example.hotelres.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    /** 정액 할인(원) */
    @Column(nullable = false)
    private Integer amount;

    /** 0/1 tinyint 매핑 */
    @Column(name = "stackable", nullable = false)
    private Boolean stackable = false;

    /** 유효 시작/종료 (NULL 허용) */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
