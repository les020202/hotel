package com.example.hotelres.admin.hotel;

public class HotelDtos {

    public record ListItem(
            Long id,
            String name,
            String region,
            String address
    ) {
        public static ListItem from(Hotel h) {
            return new ListItem(
                    h.getId(),
                    h.getName(),
                    h.getRegion(),
                    h.getAddress()
            );
        }
    }

    public record Detail(
            Long id,
            String name,
            String region,
            String address,
            String phone,
            String homepageUrl
    ) {
        public static Detail from(Hotel h) {
            return new Detail(
                    h.getId(),
                    h.getName(),
                    h.getRegion(),
                    h.getAddress(),
                    h.getPhone(),
                    h.getHomepageUrl()
            );
        }
    }

    public record UpdateReq(
            String name,
            String region,
            String address,
            String phone,
            String homepageUrl
    ) {}
}
