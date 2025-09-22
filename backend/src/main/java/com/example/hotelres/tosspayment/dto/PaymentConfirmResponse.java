// path: src/main/java/com/example/hotelres/tosspayment/dto/PaymentConfirmResponse.java
package com.example.hotelres.tosspayment.dto;

public record PaymentConfirmResponse(Long bookingId, Long paymentId, String receiptUrl) {}
