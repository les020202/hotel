package com.example.hotelres.reservation.dto;

import java.time.LocalDateTime;

// 프런트가 기대하는 필드: code, title, amount, expiresAt
public class CouponAvailableRes {
    private final String code;
    private final String title;
    private final Integer amount;
    private final LocalDateTime expiresAt;

    public CouponAvailableRes(String code, String title, Integer amount, LocalDateTime expiresAt) {
        this.code = code;
        this.title = title;
        this.amount = amount;
        this.expiresAt = expiresAt;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public Integer getAmount() { return amount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
