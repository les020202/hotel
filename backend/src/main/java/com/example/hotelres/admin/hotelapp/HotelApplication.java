package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.common.CanonicalKeyUtil;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_applications",
       indexes = {
           @Index(name="ix_app_status_region", columnList = "status, region")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HotelApplication {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false, length=50)
    private String region;   // 서울/부산/대구 (제주 제외)

    @Column(length=200)
    private String address;

    @Column(length=50)
    private String phone;

    @Column(name="homepage_url", length=300)
    private String homepageUrl;

    @Lob
    private String description;

    @Column(name="cover_image_url", length=500)
    private String coverImageUrl;

    @Column(name="canonical_key", length=64, unique = true)
    private String canonicalKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private Status status = Status.PENDING;

    @Column(name="reject_reason", length=300)
    private String rejectReason;

    @Column(name="reviewed_by")
    private Long reviewedBy;

    @Column(name="reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name="created_at", nullable=false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (canonicalKey == null || canonicalKey.isBlank()) {
            canonicalKey = CanonicalKeyUtil.forHotel(name, address);
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (canonicalKey == null || canonicalKey.isBlank()) {
            canonicalKey = CanonicalKeyUtil.forHotel(name, address);
        }
    }
}
