// src/main/java/.../coupon/dto/CouponCreateRequest.java
package com.example.hotelres.admin.coupon.dto;

import lombok.Data;

@Data
public class CouponCreateRequest {
  private String code;           // 고유 코드
  private String title;          // 표시명
  private Integer amount;        // 원 단위
  private Boolean stackable;     // 누적 사용 가능 여부
  private String validFrom;      // "2025-09-11" 형식 (DATE)
  private String validTo;        // "2025-09-30"
}
