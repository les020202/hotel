// src/main/java/com/example/hotelres/owner/sales/OwnerSalesController.java
package com.example.hotelres.owner.sales;

import com.example.hotelres.owner.sales.dto.SalesSeriesResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/hotels/{hotelId}/sales")
public class OwnerSalesController {

  private final OwnerSalesService service;

  public OwnerSalesController(OwnerSalesService service) {
    this.service = service;
  }

  // 기존: 주/월 시리즈
  // 예:
  // GET /api/owner/hotels/39/sales?mode=week
  // GET /api/owner/hotels/39/sales?mode=week&start=2025-09-23
  // GET /api/owner/hotels/39/sales?mode=month&start=2025-01-01&end=2025-12-31
  @GetMapping
  public SalesSeriesResponse getSales(
      @PathVariable Long hotelId,
      @RequestParam(defaultValue = "week") String mode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
  ) {
    return service.getSeries(hotelId, mode, start, end);
  }

  // 추가: 임의의 일자 구간을 일별로 그대로 반환 (start~end 포함)
  // 예: GET /api/owner/hotels/39/sales/daily?start=2025-09-01&end=2025-09-30
  @GetMapping("/daily")
  public SalesSeriesResponse getSalesDailyRange(
      @PathVariable Long hotelId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
  ) {
    return service.getDailyRangeSeries(hotelId, start, end);
  }
}
