package com.example.hotelres.user.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimCouponRequest(
        @NotBlank String code
) {}
