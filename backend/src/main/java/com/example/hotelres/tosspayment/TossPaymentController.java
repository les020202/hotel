// path: src/main/java/com/example/hotelres/tosspayment/TossPaymentController.java
package com.example.hotelres.tosspayment;

import com.example.hotelres.tosspayment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class TossPaymentController {
    private final PaymentsOrchestrator orchestrator;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(@Valid @RequestBody ConfirmRequest req) {
        var out = orchestrator.confirmToss(req.paymentKey(), req.orderId(), req.amount(), req.holdCode());
        return ResponseEntity.ok(out);
    }
}
