package com.example.hotelres.user.coupon.dto;

public record UserCouponDto(
        Long id, String code, String title, int amount, boolean stackable,
        String status, String issuedAt, String validFrom, String validTo) {}
