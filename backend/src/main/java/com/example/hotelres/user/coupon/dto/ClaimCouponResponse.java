package com.example.hotelres.user.coupon.dto;

public record ClaimCouponResponse(
        Long issuanceId,
        String code,
        String title,
        Integer amount,
        Boolean stackable,
        String validFrom,  // "YYYY-MM-DD" (null 가능)
        String validTo,    // "YYYY-MM-DD" (null 가능)
        String status      // "AVAILABLE"
) {}
