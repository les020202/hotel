// src/main/java/.../coupon/CouponService.java
package com.example.hotelres.admin.coupon;

import com.example.hotelres.admin.coupon.dto.CouponCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository repo;

  /* 공통 유효성 검사 */
  private void validate(CouponCreateRequest req) {
    if (req.getCode() == null || req.getCode().isBlank())
      throw new IllegalArgumentException("쿠폰 코드를 입력하세요.");
    if (req.getTitle() == null || req.getTitle().isBlank())
      throw new IllegalArgumentException("쿠폰명을 입력하세요.");
    if (req.getAmount() == null || req.getAmount() <= 0)
      throw new IllegalArgumentException("amount는 1원 이상이어야 합니다.");

    LocalDate from = parse(req.getValidFrom());
    LocalDate to   = parse(req.getValidTo());
    if (from != null && to != null && to.isBefore(from))
      throw new IllegalArgumentException("종료일이 시작일보다 빠릅니다.");
  }

  private LocalDate parse(String s) {
    return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
  }

  /* 생성 */
  @Transactional
  public Coupon create(CouponCreateRequest req) {
    validate(req);

    String code = req.getCode().trim();
    if (repo.existsByCode(code))
      throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다.");

    Coupon c = Coupon.builder()
        .code(code)
        .title(req.getTitle().trim())
        .amount(req.getAmount())
        .stackable(Boolean.TRUE.equals(req.getStackable()))
        .validFrom(parse(req.getValidFrom()))
        .validTo(parse(req.getValidTo()))
        .build();

    return repo.save(c);
  }

  /* 수정 */
  @Transactional
  public Coupon update(Long id, CouponCreateRequest req) {
    validate(req);

    Coupon c = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + id));

    String newCode = req.getCode().trim();
    // 코드가 변경될 때만 중복 검사
    if (!newCode.equals(c.getCode()) && repo.existsByCode(newCode))
      throw new IllegalArgumentException("이미 존재하는 쿠폰 코드입니다.");

    c.setCode(newCode);
    c.setTitle(req.getTitle().trim());
    c.setAmount(req.getAmount());
    c.setStackable(Boolean.TRUE.equals(req.getStackable()));
    c.setValidFrom(parse(req.getValidFrom()));
    c.setValidTo(parse(req.getValidTo()));

    return repo.save(c); // 또는 @Transactional 이므로 return c; 도 가능
  }

  /* 삭제 */
  @Transactional
  public void delete(Long id) {
    if (!repo.existsById(id))
      throw new IllegalArgumentException("존재하지 않는 쿠폰입니다. id=" + id);
    repo.deleteById(id);
  }
}
