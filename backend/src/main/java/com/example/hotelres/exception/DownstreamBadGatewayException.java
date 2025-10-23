package com.example.hotelres.exception;

public class DownstreamBadGatewayException extends RuntimeException {
    public DownstreamBadGatewayException() { super(); }
    public DownstreamBadGatewayException(String msg) { super(msg); }
}
