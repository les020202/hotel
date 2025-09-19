package com.example.hotelres.main.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Hotel {
    private Long id;

    // 기본 정보
    private String name;
    private Double rating;              // null 가능
    private String officialGrade;
    private Integer gradeLevel;

    private String region;
    private String address;
    private String phone;
    private String homepageUrl;

    private Double latitude;
    private Double longitude;

    private String canonicalKey;

    // 이미지
    private String coverImageType;      // NONE | UPLOADED | TEMPLATE
    private String coverImageUrl;
    private String coverImageTemplate;  // C1 | C2 | C3
}
