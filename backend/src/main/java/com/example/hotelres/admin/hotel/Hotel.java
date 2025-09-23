package com.example.hotelres.admin.hotel;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotels")  // ★ 중요: 실제 테이블명과 맞추기
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
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

    @Column(name = "homepage_url", length = 300)
    private String homepageUrl;

    // DDL: DECIMAL(2,1)
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "official_grade", length = 20)
    private String officialGrade;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
