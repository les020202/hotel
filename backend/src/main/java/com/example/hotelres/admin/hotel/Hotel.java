// src/main/java/com/example/hotelres/admin/hotel/Hotel.java
package com.example.hotelres.admin.hotel;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotels")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /** 등급 레벨 (1~5 등급 같은 정수값) */
    @Column(name = "grade_level")
    private Integer gradeLevel;

    /** 관광공사 등에서 부여한 공식 등급 (예: 5성급) */
    @Column(name = "official_grade")
    private String officialGrade;

    /** 커버 이미지 타입 */
    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_type")
    private CoverImageType coverImageType;

    /** 커버 이미지 URL */
    @Column(length = 500)
    private String coverImageUrl;

    /** 커버 이미지 템플릿 */
    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_template")
    private CoverImageTemplate coverImageTemplate;

    /** 호텔 홈페이지 URL */
    @Column(name = "homepage_url", length = 300)
    private String homepageUrl;

    /** 지도 좌표 */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /** 호텔을 유일하게 식별할 키 */
    @Column(name = "canonical_key", length = 64, unique = true)
    private String canonicalKey;

    // --- Enum 정의 ---
    public enum CoverImageType { NONE, UPLOADED, TEMPLATE }
    public enum CoverImageTemplate { DEFAULT, BEACH, CITY }
}
