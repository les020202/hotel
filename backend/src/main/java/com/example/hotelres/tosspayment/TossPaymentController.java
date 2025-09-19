package com.example.hotelres.tosspayment;

import com.example.hotelres.tosspayment.dto.ConfirmRequest;
import com.example.hotelres.tosspayment.dto.PaymentConfirmResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/toss")
public class TossPaymentController {

    private final PaymentsOrchestrator orchestrator;

    public TossPaymentController(PaymentsOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(@RequestBody ConfirmRequest body) {
        PaymentConfirmResponse res = orchestrator.confirmToss(
                body.getPaymentKey(), body.getOrderId(), body.getAmount(), body.getHoldCode()
        );
        return ResponseEntity.ok(res);
    }
}
