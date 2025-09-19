// path: src/main/java/com/example/hotelres/reservation/SoldOutException.java
package com.example.hotelres.reservation;

public class SoldOutException extends RuntimeException {
    public SoldOutException(String message) { super(message); }
}
