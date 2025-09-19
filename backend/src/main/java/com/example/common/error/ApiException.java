package com.example.common.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final int status;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
