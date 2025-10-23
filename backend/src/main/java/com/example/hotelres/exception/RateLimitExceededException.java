package com.example.hotelres.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() { super(); }
    public RateLimitExceededException(String msg) { super(msg); }
}
