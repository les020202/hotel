// src/main/java/com/example/hotelres/settlement/SettlementExceptionHandler.java
package com.example.hotelres.settlement;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class SettlementExceptionHandler {

    @ExceptionHandler(CutoffNotReachedException.class)
    public ResponseEntity<?> handleCutoff(CutoffNotReachedException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT) // 409
            .body(new ErrorResponse("정산 확정은 " + ex.getMessage() + " 이후 가능합니다."));
    }

    static record ErrorResponse(String message) {}
}
