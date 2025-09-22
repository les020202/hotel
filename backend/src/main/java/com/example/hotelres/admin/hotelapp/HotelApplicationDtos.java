package com.example.hotelres.admin.hotelapp;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.example.hotelres.admin.hotelapp.HotelApplication.Status;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class AppListItemDto {
    private Long id;
    private String name;
    private String region;
    private Status status;
    private Long applicantUserId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static AppListItemDto from(HotelApplication a) {
        return AppListItemDto.builder()
                .id(a.getId())
                .name(a.getName())
                .region(a.getRegion())
                .status(a.getStatus())
                .applicantUserId(a.getApplicantUserId())
                .createdAt(a.getCreatedAt())
                .reviewedAt(a.getReviewedAt())
                .build();
    }
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class AppDetailDto {
    private Long id;
    private String name;
    private String region;
    private String address;
    private String phone;
    private String homepageUrl;
    private String description;
    private String coverImageUrl;
    private String canonicalKey;
    private HotelApplication.Status status;
    private String rejectReason;
    private Long applicantUserId;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AppDetailDto from(HotelApplication a) {
        return AppDetailDto.builder()
                .id(a.getId())
                .name(a.getName())
                .region(a.getRegion())
                .address(a.getAddress())
                .phone(a.getPhone())
                .homepageUrl(a.getHomepageUrl())
                .description(a.getDescription())
                .coverImageUrl(a.getCoverImageUrl())
                .canonicalKey(a.getCanonicalKey())
                .status(a.getStatus())
                .rejectReason(a.getRejectReason())
                .applicantUserId(a.getApplicantUserId())
                .reviewedBy(a.getReviewedBy())
                .reviewedAt(a.getReviewedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

@Getter @Setter
class ApproveRequest {
    private String note;    // 심사 메모 (감사로그용)
}

@Getter @Setter
class RejectRequest {
    private String reason;  // 반드시 저장
    private String note;    // 감사로그용 (선택)
}

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
class PagedResp<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PagedResp<T> of(org.springframework.data.domain.Page<T> p) {
        return PagedResp.<T>builder()
                .content(p.getContent())
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .build();
    }
}
