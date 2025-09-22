package com.example.hotelres.payment.dto;

public class ConfirmRequest {
    private String paymentKey;
    private String orderId;
    private long amount; // 숫자! (문자열 아님)

    public String getPaymentKey() { return paymentKey; }
    public void setPaymentKey(String paymentKey) { this.paymentKey = paymentKey; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
