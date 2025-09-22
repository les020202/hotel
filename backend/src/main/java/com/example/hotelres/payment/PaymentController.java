package com.example.hotelres.payment;

import com.example.hotelres.payment.dto.ConfirmRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PaymentController {

    private final TossClient tossClient;

    public PaymentController(TossClient tossClient) {
        this.tossClient = tossClient;
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirm(@RequestBody ConfirmRequest req) {
        try {
            // 1) 토스 결제 확인
            Map<String, Object> tossResp = tossClient.confirm(req);

            // 2) TODO: 여기서 예약 재고 확정(비관적 락 or 원자적 UPDATE) 수행
            // reservationService.confirmPaid(orderId, paymentKey, amount);

            // 3) 프론트가 항상 JSON을 받도록 성공 응답 구성
            Map<String, Object> body = new HashMap<>();
            body.put("result", "PAID");
            body.put("orderId", req.getOrderId());
            body.put("paymentKey", req.getPaymentKey());
            body.put("amount", req.getAmount());
            body.put("toss", tossResp); // 필요 시만 포함

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            // 실패 시에도 항상 JSON으로!
            Map<String, Object> err = new HashMap<>();
            err.put("result", "FAIL");
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
