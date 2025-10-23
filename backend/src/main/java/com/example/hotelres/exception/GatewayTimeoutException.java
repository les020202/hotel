package com.example.hotelres.exception;

public class GatewayTimeoutException extends RuntimeException {
    public GatewayTimeoutException() { super(); }
    public GatewayTimeoutException(String msg) { super(msg); }
}
