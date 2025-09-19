package com.example.hotelres.main.dto;

import lombok.Data;

@Data
public class HotelSearchParams {
    private String q;
    private String region;
    private Integer gradeMin;
    private Boolean hasHomepage;
    private Integer page = 0;
    private Integer size = 12;
}
