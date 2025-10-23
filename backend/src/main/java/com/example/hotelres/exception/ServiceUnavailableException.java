package com.example.hotelres.exception;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException() { super(); }
    public ServiceUnavailableException(String msg) { super(msg); }
}
