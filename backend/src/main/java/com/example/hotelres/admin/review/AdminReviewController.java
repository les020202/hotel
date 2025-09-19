// src/main/java/.../admin/review/AdminReviewController.java
package com.example.hotelres.admin.review;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

  private final AdminReviewRepository repo;

  /* 리뷰 탭 */
  @GetMapping
  public List<AdminReviewRepository.ReviewRow> listReviews(
      @RequestParam(required=false) String q,
      @RequestParam(required=false) Integer ratingMin,
      @RequestParam(required=false) Integer ratingMax,
      @RequestParam(required=false) Integer visible, // 1/0
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="50") int size
  ){
    int limit = Math.max(1, Math.min(200, size));
    int offset = Math.max(0, page) * limit;
    return repo.findReviews(emptyToNull(q), ratingMin, ratingMax, visible, limit, offset);
  }

  /* 신고 탭 */
  @GetMapping("/reports")
  public List<AdminReviewRepository.ReportRow> listReports(
      @RequestParam(required=false) String q,
      @RequestParam(defaultValue="false") boolean ownerOnly,
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="50") int size
  ){
    int limit = Math.max(1, Math.min(200, size));
    int offset = Math.max(0, page) * limit;
    return repo.findReports(emptyToNull(q), ownerOnly, limit, offset);
  }

  /* 리뷰 삭제(하드) */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id){
    int n = repo.hardDelete(id);
    return (n>0) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  private String emptyToNull(String s){ return (s==null || s.isBlank()) ? null : s; }
}
