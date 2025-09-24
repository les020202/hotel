package com.example.hotelres.tosspayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConfirmRequest(
    @NotBlank String paymentKey,
    @NotBlank String orderId,
    @Positive long amount,      // int → long
    String holdCode             // @NotBlank 제거 (optional)
) {}
