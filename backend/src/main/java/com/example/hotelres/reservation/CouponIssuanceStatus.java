// path: backend/src/main/java/com/example/hotelres/reservation/CouponIssuanceStatus.java
package com.example.hotelres.reservation;

public enum CouponIssuanceStatus {
    AVAILABLE,  // 사용 가능
    USED,       // 사용됨
    EXPIRED     // 만료됨 (관리용)
}
