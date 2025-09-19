package com.example.hotelres.amenity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AmenityDto {
    private Long id;
    private String code;
    private String name;
    private Integer sortOrder;
}
