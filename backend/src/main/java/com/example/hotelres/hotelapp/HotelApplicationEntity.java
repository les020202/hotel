package com.example.hotelres.hotelapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_applications")
@Getter @Setter
@NoArgsConstructor
public class HotelApplicationEntity {

    public enum Status {
        PENDING, UNDER_REVIEW, NEEDS_INFO, APPROVED, REJECTED;

        public static Status from(String s) {
            if (s == null || s.isBlank()) return null;
            try { return Status.valueOf(s.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { return null; }
        }
    }

    public enum CoverImageType { NONE, UPLOADED, TEMPLATE }
    public enum CoverImageTemplate { C1, C2, C3 } // 스키마 ENUM과 동일

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "owner_name", nullable = false, length = 50)
    private String ownerName;

    @Column(name = "business_no", nullable = false, length = 20)
    private String businessNo;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    // TINYINT → Integer
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "hotel_name", nullable = false, length = 100)
    private String hotelName;

    // ✅ region 추가
    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "address1", nullable = false, length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Column(name = "postcode", length = 10)
    private String postcode;

    // ✅ VARCHAR(500)이므로 @Lob 제거
    @Column(name = "amenities_csv", length = 500)
    private String amenitiesCsv;

    // MariaDB JSON → 문자열로 보관
    @Column(name = "rooms_json", columnDefinition = "JSON")
    private String roomsJson;

    // ✅ VARCHAR(1000)이므로 @Lob 제거
    @Column(name = "comment", length = 1000)
    private String comment;

    // 심사 정보
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_memo", length = 1000)
    private String reviewMemo;

    // ✅ 승인된 호텔 ID(프로시저가 채움)
    @Column(name = "approved_hotel_id")
    private Long approvedHotelId;

    // ✅ 대표 이미지 필드들 (프런트에서 URL로 보냄)
    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_type", nullable = false, length = 16)
    private CoverImageType coverImageType = CoverImageType.NONE;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_image_template", length = 8)
    private CoverImageTemplate coverImageTemplate; // 템플릿 미사용이면 null

    // 타임스탬프
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = Status.PENDING;
        if (coverImageType == null) coverImageType = CoverImageType.NONE;
    }
}
