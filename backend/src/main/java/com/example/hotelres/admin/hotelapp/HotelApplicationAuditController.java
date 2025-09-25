// com.example.hotelres.admin.hotelapp.HotelApplicationAuditController
package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.hotelapp.HotelApplicationEntity;
import com.example.hotelres.security.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotelapp")
@RequiredArgsConstructor
public class HotelApplicationAuditController {

  private final HotelApplicationAuditService svc;

  @GetMapping
  public Page<HotelApplicationEntity> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return svc.list(status, q, page, size);
  }

  @GetMapping("/{id}")
  public ResponseEntity<HotelApplicationEntity> get(@PathVariable Long id) {
    var data = svc.get(id);
    return data == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(data);
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<Void> approve(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails admin) {
    svc.approve(id, admin);
    return ResponseEntity.ok().build();
  }

  public record RejectReq(@NotBlank String reason, String note) {}
  @PostMapping("/{id}/reject")
  public ResponseEntity<Void> reject(
      @PathVariable Long id,
      @RequestBody RejectReq req,
      @AuthenticationPrincipal CustomUserDetails admin) {
    svc.reject(id, req.reason(), admin);
    return ResponseEntity.ok().build();
  }
}
