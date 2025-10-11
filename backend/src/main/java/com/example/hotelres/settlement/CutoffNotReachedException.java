package com.example.hotelres.settlement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class CutoffNotReachedException extends RuntimeException {
    public CutoffNotReachedException(String message) {
        super(message);
    }

    public CutoffNotReachedException(String cutoff, String now) {
        super("정산 확정은 " + cutoff + " 이후에만 가능합니다. (현재: " + now + ")");
    }
}

