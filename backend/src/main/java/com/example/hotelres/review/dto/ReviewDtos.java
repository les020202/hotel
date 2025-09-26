package com.example.hotelres.review.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReviewItem {
        private Long id;
        private Long hotelId;
        private Long bookingId;
        private Long userId;
        private short rating;
        private String comment;
        private boolean visible;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<String> photos; // 리뷰 사진들

        // ===== 뷰 표시용 메타 =====
        private String reviewerName;
        private String profileImageType;     // NONE/UPLOADED/TEMPLATE
        private String profileImageUrl;      // UPLOADED일 때
        private String profileImageTemplate; // TEMPLATE일 때 (T1/T2/T3)

        private String roomTypeName;         // 실제 투숙 룸타입명
        private LocalDate checkIn;
        private LocalDate checkOut;

        private String hotelName;   // ← 반드시 추가
        private String userName;    // ← 반드시 추가
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ListResponse {
        private List<ReviewItem> content;
        private long totalElements;
        private int totalPages;
        private int number;
        private int size;
        private Double avgRating;
        private long count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EligibilityResponse {
        private boolean eligible;
        private Long bookingId;
        private String reason;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RatingResponse {
        private double avg;
        private long count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReportRequest {
        private String reason;
        private String detail;        // ★ 추가: 상세 사유
        private String reporterType;
    }
}
