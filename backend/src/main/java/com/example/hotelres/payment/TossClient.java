package com.example.hotelres.payment;

import com.example.hotelres.payment.dto.ConfirmRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class TossClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String authHeader;

    public TossClient(@Value("${toss.secret-key}") String secretKey) {
        String basic = secretKey + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(basic.getBytes(StandardCharsets.UTF_8));
        this.authHeader = "Basic " + encoded;
    }

    public Map<String, Object> confirm(ConfirmRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "paymentKey", req.getPaymentKey(),
                "orderId",    req.getOrderId(),
                "amount",     req.getAmount()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> res = restTemplate.exchange(
                "https://api.tosspayments.com/v1/payments/confirm",
                HttpMethod.POST,
                entity,
                Map.class
        );
        return res.getBody();
    }
}
