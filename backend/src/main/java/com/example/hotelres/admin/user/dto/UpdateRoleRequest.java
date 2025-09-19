package com.example.hotelres.admin.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateRoleRequest {
  private String role; // ROLE_USER / ROLE_ADMIN / ROLE_OWNER
}
