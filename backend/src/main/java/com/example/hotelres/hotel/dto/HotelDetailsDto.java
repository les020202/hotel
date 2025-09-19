
package com.example.hotelres.hotel.dto;

import lombok.Data;
import java.util.List;

@Data
public class HotelDetailsDto {
    private Hotel hotel;
    private Gallery gallery;
    private List<RoomTypeOffer> roomTypes;
    private List<Amenity> amenities;

    @Data public static class Hotel {
        private Long id;
        private String name;
        private String region;
        private String address;
        private Double rating;
        private Integer gradeLevel;
        private String coverImageUrl; // 필요 시

        private String phone;
        private Double latitude;
        private Double longitude;
    }
    @Data public static class Gallery {
        private String cover;
        private List<RoomImage> roomDefaults;
    }
    @Data public static class RoomImage {
        private String url;
        private Integer sortOrder;
        private String altText;
    }
    @Data public static class RoomTypeOffer {
        private Long roomTypeId;
        private String name;
        private Integer capacity;
        private Integer areaSqm;
        private Integer minRemaining;
        private Long priceSum;
        private Integer nights;
        private String templateImageUrl;
    }
    @Data public static class Amenity {
        private String code;
        private String name;
    }
}
