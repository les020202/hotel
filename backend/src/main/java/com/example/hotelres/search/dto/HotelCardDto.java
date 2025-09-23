package com.example.hotelres.search.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HotelCardDto {
    private Long   hotelId;
    private String name;
    private String address;
    private String coverImageUrl;
    private Double rating;          // hotels.rating
    private Integer gradeLevel;     // 정렬 보조
    private Long   startingFrom;    // 총액 (KRW)
    private Integer imagesCount;    // 항상 12
    private Integer amenitiesCount;
}
