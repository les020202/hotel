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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status = Status.PENDING; // DB 기본값과 동일

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "owner_name", nullable = false, length = 50)
    private String ownerName;

    @Column(name = "business_no", nullable = false, length = 20)
    private String businessNo;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    // DB가 TINYINT 이므로 Integer로 맞춤
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "hotel_name", nullable = false, length = 100)
    private String hotelName;

    @Column(name = "address1", nullable = false, length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Lob
    @Column(name = "amenities_csv")
    private String amenitiesCsv;

    // MariaDB의 JSON은 LONGTEXT alias. 그대로 사용해도 OK
    @Column(name = "rooms_json", columnDefinition = "JSON")
    private String roomsJson;

    @Lob
    @Column(name = "comment")
    private String comment;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_memo", length = 1000)
    private String reviewMemo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = Status.PENDING;
    }
}
