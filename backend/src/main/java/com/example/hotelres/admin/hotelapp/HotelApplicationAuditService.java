package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.hotelapp.HotelApplicationEntity;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class HotelApplicationAuditService {

    private final HotelApplicationAuditRepository repo;

    public HotelApplicationAuditService(HotelApplicationAuditRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<HotelApplicationEntity> list(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        HotelApplicationEntity.Status st = HotelApplicationEntity.Status.from(status); // valueOf X
        if (st == null) return repo.findAllByOrderByIdDesc(pageable);
        return repo.findByStatusOrderByIdDesc(st, pageable);
    }

    @Transactional(readOnly = true)
    public HotelApplicationEntity get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found: " + id));
    }

    @Transactional
    public HotelApplicationEntity markUnderReview(Long id, Long adminId, String memo) {
        var e = get(id);
        e.setStatus(HotelApplicationEntity.Status.UNDER_REVIEW);
        e.setReviewedBy(adminId);
        e.setReviewedAt(LocalDateTime.now());
        e.setReviewMemo(memo);
        return e;
    }

    @Transactional
    public HotelApplicationEntity approve(Long id, Long adminId, String memo) {
        var e = get(id);
        e.setStatus(HotelApplicationEntity.Status.APPROVED);
        e.setReviewedBy(adminId);
        e.setReviewedAt(LocalDateTime.now());
        e.setReviewMemo(memo);
        // TODO: 필요시 hotels/room_types 생성 로직 연결
        return e;
    }

    @Transactional
    public HotelApplicationEntity reject(Long id, Long adminId, String memo) {
        var e = get(id);
        e.setStatus(HotelApplicationEntity.Status.REJECTED);
        e.setReviewedBy(adminId);
        e.setReviewedAt(LocalDateTime.now());
        e.setReviewMemo(memo);
        return e;
    }
}
