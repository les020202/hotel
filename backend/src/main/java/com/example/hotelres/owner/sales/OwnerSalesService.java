// src/main/java/com/example/hotelres/owner/sales/OwnerSalesService.java
package com.example.hotelres.owner.sales;

import com.example.hotelres.owner.sales.dto.SalesPoint;
import com.example.hotelres.owner.sales.dto.SalesSeriesResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OwnerSalesService {

  private final OwnerSalesRepository repo;

  public OwnerSalesService(OwnerSalesRepository repo) {
    this.repo = repo;
  }

  public SalesSeriesResponse getSeries(Long hotelId, String mode, LocalDate start, LocalDate end) {
    String m = normalize(mode);

    if ("week".equals(m)) {
      LocalDate monday = (start != null) ? toMonday(start) : currentMonday();
      List<Object[]> raw = repo.weekByDay(hotelId, monday); // 항상 7행
      return toResponse("week", raw, monday, monday.plusDays(6));
    }

    // month
    LocalDate startMonth = (start != null) ? start.withDayOfMonth(1)
                                           : LocalDate.now().minusMonths(11).withDayOfMonth(1);
    LocalDate endMonthEx = (end != null) ? end.plusMonths(1).withDayOfMonth(1)
                                         : LocalDate.now().plusMonths(1).withDayOfMonth(1);
    List<Object[]> raw = repo.monthSeries(hotelId, startMonth, endMonthEx);
    return toResponse("month", raw, startMonth, endMonthEx.minusDays(1));
  }

  // 'day'로 들어오면 week로 우회
  private String normalize(String mode) {
    if (mode == null) return "week";
    mode = mode.trim().toLowerCase();
    return switch (mode) {
      case "month", "monthly", "m" -> "month";
      default -> "week";
    };
  }

  private LocalDate currentMonday() {
    return toMonday(LocalDate.now());
  }
  private LocalDate toMonday(LocalDate d) {
    DayOfWeek dow = d.getDayOfWeek();
    return d.minusDays(dow.getValue() - DayOfWeek.MONDAY.getValue());
  }

  private SalesSeriesResponse toResponse(String mode, List<Object[]> raw, LocalDate start, LocalDate end) {
    List<SalesPoint> items = new ArrayList<>();
    for (Object[] r : raw) {
      items.add(new SalesPoint(String.valueOf(r[0]), ((Number) r[1]).longValue()));
    }
    return new SalesSeriesResponse(mode, start, end, items);
  }
}
