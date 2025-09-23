// path: src/main/java/com/example/hotelres/tosspayment/dto/TossConfirmResponse.java
package com.example.hotelres.tosspayment.dto;

import java.util.Map;

/** Toss confirm 응답에서 사용하는 필드만 선언 */
public record TossConfirmResponse(
        String mId,
        String paymentKey,
        String orderId,
        String orderName,
        String status,          // DONE, CANCELED, ...
        String requestedAt,
        String approvedAt,
        String currency,        // KRW
        Integer totalAmount,
        Integer balanceAmount,
        Integer suppliedAmount,
        Integer vat,
        Integer taxFreeAmount,
        String method,          // "간편결제" 등
        Map<String,Object> card,
        Map<String,Object> virtualAccount,
        Map<String,Object> transfer,
        Map<String,Object> easyPay,
        Map<String,Object> receipt // { url: "..." }
) {}
