// src/main/java/com/example/hotelres/settlement/repo/HotelPayoutView.java
package com.example.hotelres.settlement.repo;

/** hotels 테이블의 지급정보만 읽기 위한 Projection */
public interface HotelPayoutView {
    String getPayoutBankCode();
    String getPayoutAccountNo();
    String getPayoutHolderName();
}
