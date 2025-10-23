package com.example.hotelres.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final int status;
    private final String code;     // ex) "INVALID_REQUEST", "UNAUTHORIZED"
    private final String message;  // 사용자에게 보여줄 짧은 메시지
    private final String errorId;  // 로그 추적용 ID

    public ErrorResponse(int status, String code, String message, String errorId) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.errorId = errorId;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getErrorId() { return errorId; }

    public static ErrorResponse of(int status, String code, String message, String errorId) {
        return new ErrorResponse(status, code, message, errorId);
    }
}
