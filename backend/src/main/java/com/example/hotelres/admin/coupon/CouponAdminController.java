package com.example.hotelres.admin.coupon;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.hotelres.admin.coupon.dto.CouponCreateRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 클래스 전체 보호 (메서드마다 붙일 필요 X)
public class CouponAdminController {

  private final CouponRepository repo;
  private final CouponService svc;

  @GetMapping
  public List<Coupon> list() {
    return repo.findAllByOrderByCreatedAtDesc();
  }

  @PostMapping
  public ResponseEntity<Coupon> create(@RequestBody CouponCreateRequest req) {
    return ResponseEntity.ok(svc.create(req));
  }

  // ✅ 절대경로가 아니라 상대경로만!
  @PutMapping("/{id}")
  public ResponseEntity<Coupon> update(@PathVariable Long id,
                                       @RequestBody CouponCreateRequest req) {
    return ResponseEntity.ok(svc.update(id, req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    svc.delete(id);
    return ResponseEntity.noContent().build();
  }
}
