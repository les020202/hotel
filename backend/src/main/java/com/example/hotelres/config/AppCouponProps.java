package com.example.hotelres.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.coupon")
public record AppCouponProps(
        String welcomeCode,
        int welcomeExpDays
) {}
