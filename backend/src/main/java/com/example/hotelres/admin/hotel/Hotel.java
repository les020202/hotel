// src/main/java/com/example/hotelres/admin/hotel/Hotel.java
package com.example.hotelres.admin.hotel;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hotel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String region;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String phone;

    /** 호텔 평점 (예: 4.5) */
    @Column(precision = 2, scale = 1) // DECIMAL(2,1)
    private BigDecimal rating;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @Column(name = "official_grade")
    private String officialGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_type")
    private CoverImageType coverImageType;

    @Column(length = 500, name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_template")
    private CoverImageTemplate coverImageTemplate;

    @Column(name = "homepage_url", length = 300)
    private String homepageUrl;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "canonical_key", length = 64, unique = true)
    private String canonicalKey;

    /* ───────────────────── 정산용 추가 컬럼 ───────────────────── */

    /** 플랫폼 수수료율 (예: 0.1500 = 15%) */
    @Column(name = "settlement_fee_pct", precision = 5, scale = 4, nullable = false)
    private BigDecimal settlementFeePct;   // DB default 0.150, null일 수 있으니 서비스에서 기본값 보정 가능

    
    /* ─────────────────────────────────────────────────────────── */

    public enum CoverImageType { NONE, UPLOADED, TEMPLATE }
    public enum CoverImageTemplate { DEFAULT, BEACH, CITY }

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
        // DB 기본값이 잡히지 않았을 때의 안전장치 (선택)
        if (settlementFeePct == null) settlementFeePct = new BigDecimal("0.1500");
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
