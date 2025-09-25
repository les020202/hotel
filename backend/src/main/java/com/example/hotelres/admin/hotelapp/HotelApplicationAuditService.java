// com.example.hotelres.admin.hotelapp.HotelApplicationAuditService
package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.hotelapp.HotelApplicationEntity;
import com.example.hotelres.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HotelApplicationAuditService {

  private final HotelApplicationAuditRepository repo;

  public Page<HotelApplicationEntity> list(String status, String q, int page, int size) {
    var st = HotelApplicationEntity.Status.from(status);
    var qq = (q == null || q.isBlank()) ? null : q.trim();
    return repo.adminSearch(st, qq, PageRequest.of(page, size));
  }

  public HotelApplicationEntity get(Long id) {
    return repo.findById(id).orElse(null);
  }

  @Transactional
  public void approve(Long id, CustomUserDetails admin) {
    var app = repo.findById(id).orElseThrow();
    app.setStatus(HotelApplicationEntity.Status.APPROVED);
    app.setReviewedBy(admin != null ? admin.getId() : null);
    app.setReviewedAt(LocalDateTime.now());
    app.setReviewMemo("APPROVED");
  }

  @Transactional
  public void reject(Long id, String reason, CustomUserDetails admin) {
    var app = repo.findById(id).orElseThrow();
    app.setStatus(HotelApplicationEntity.Status.REJECTED);
    app.setReviewedBy(admin != null ? admin.getId() : null);
    app.setReviewedAt(LocalDateTime.now());
    app.setReviewMemo(reason == null ? "" : reason.trim());
  }
}
