package com.example.hotelres.admin.hotel;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHotelService {
  private final HotelRepository hotelRepo;

  public Page<Hotel> list(String region, String q, int page, int size) {
    String rg = region == null ? "" : region.trim();
    String qq = q == null ? "" : q.trim();
    int p = Math.max(page, 0);
    int s = Math.min(Math.max(size, 1), 2000); // 안전 상한
    return hotelRepo.search(rg, qq, PageRequest.of(p, s));
  }

  @Transactional
  public Hotel update(Long id, Hotel patch) {
    Hotel h = hotelRepo.findById(id).orElseThrow();
    if (patch.getName()   != null) h.setName(patch.getName());
    if (patch.getRegion() != null) h.setRegion(patch.getRegion());
    if (patch.getAddress()!= null) h.setAddress(patch.getAddress());
    if (patch.getPhone()  != null) h.setPhone(patch.getPhone());
    return hotelRepo.save(h);
  }

  @Transactional
  public void delete(Long id) {
    // 의존 데이터가 있으면 FK 제약/트리거로 409 나갈 수 있음 → 서비스에서 잡아 409로 변환해도 됨
    hotelRepo.deleteById(id);
  }
}
