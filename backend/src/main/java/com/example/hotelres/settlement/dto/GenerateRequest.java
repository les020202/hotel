// dto/GenerateRequest.java
package com.example.hotelres.settlement.dto;

import java.time.*;
public record GenerateRequest(
  Long hotelId, LocalDate start, LocalDate end,
  String bankCode, String accountNo, String holderName
) {}
