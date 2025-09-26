// src/main/java/com/example/hotelres/review/dto/AdminReviewDtos.java
package com.example.hotelres.review.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminReviewDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Row {
        private Long id;
        private Long hotelId;
        private Long userId;

        private String hotelName;   // ← 리스트에 표시
        private String userName;    // ← 리스트에 표시

        private Boolean visible;    // 배지용
        private LocalDate createdDate;  // YYYY-MM-DD만 (프론트 요구)
        // 선택: 그리드에서 쓰진 않지만 보존
        private Short rating;
        private String comment;

        // 신고 요약(필요시 컬럼 숨길 수 있음)
        private Integer reportCount;
        private Integer userReportCount;
        private Integer ownerReportCount;
        private LocalDateTime latestReportAt;
    }
}
