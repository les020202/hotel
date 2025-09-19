// src/main/java/com/example/hotelres/search/dto/HotelSuggestItem.java
package com.example.hotelres.search.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HotelSuggestItem {
    Long hotelId;
    String name;
    String region;
    String address;
    String coverImageUrl; // nullable ok
}
