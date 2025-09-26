// dto/WeekRange.java
package com.example.hotelres.settlement.dto;

import java.time.*;
public record WeekRange(LocalDate start, LocalDate end) {
  public static LocalDate mondayOf(LocalDate d) { var wd=d.getDayOfWeek().getValue(); return d.minusDays((wd+6)%7); }
  public static LocalDate sundayOf(LocalDate d) { return mondayOf(d).plusDays(6); }
}
