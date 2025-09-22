// path: src/main/java/com/example/hotelres/tosspayment/dto/ConfirmRequest.java
package com.example.hotelres.tosspayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConfirmRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @Positive int amount,
        @NotBlank String holdCode
) {}
