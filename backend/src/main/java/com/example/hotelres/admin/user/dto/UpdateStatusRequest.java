package com.example.hotelres.admin.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateStatusRequest {
  private String status; // ACTIVE / LOCKED / INACTIVE
}
