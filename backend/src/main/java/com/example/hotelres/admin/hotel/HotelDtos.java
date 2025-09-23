package com.example.hotelres.admin.hotel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HotelDtos {

    public record ListItem(
            Long id, String name, String region, String address
    ){
        public static ListItem from(Hotel h) {
            return new ListItem(
                h.getId(), h.getName(), h.getRegion(), h.getAddress()
            );
        }
    }

    public record Detail(
            Long id, String name, String region, String address,
            String phone, String homepageUrl,
            BigDecimal rating, String officialGrade, Integer gradeLevel,
            String coverImageUrl,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ){
        public static Detail from(Hotel h){
            return new Detail(
                h.getId(), h.getName(), h.getRegion(), h.getAddress(),
                h.getPhone(), h.getHomepageUrl(),
                h.getRating(), h.getOfficialGrade(), h.getGradeLevel(),
                h.getCoverImageUrl(),
                h.getCreatedAt(), h.getUpdatedAt()
            );
        }
    }

    public record UpdateReq(
            String name, String region, String address, String phone, String homepageUrl
    ){}
}
