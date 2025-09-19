package com.example.hotelres.admin.hotel;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
public class AdminHotelController {
  private final AdminHotelService svc;

  @GetMapping
  public Page<Hotel> list(
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "1000") int size
  ) {
    return svc.list(region, q, page, size);
  }

  @PutMapping("/{id}")
  public Hotel update(@PathVariable Long id, @RequestBody Hotel patch) {
    return svc.update(id, patch);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      svc.delete(id);
      return ResponseEntity.noContent().build();
    } catch (DataIntegrityViolationException e) {
      // 예약, 요금, 객실 등 FK가 걸려 있으면 여기로 들어옴 → 409로 응답
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }
}
