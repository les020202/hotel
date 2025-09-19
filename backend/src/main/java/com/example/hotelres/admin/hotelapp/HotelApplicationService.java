package com.example.hotelres.admin.hotelapp;

import com.example.hotelres.admin.hotelapp.HotelApplication.Status;
import com.example.hotelres.common.CanonicalKeyUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class HotelApplicationService {

    private final HotelApplicationRepository repo;
    private final HotelApplicationAuditRepository auditRepo;

    @PersistenceContext
    private EntityManager em;

    /* -------------------- 조회 -------------------- */

    @Transactional(Transactional.TxType.SUPPORTS)
    public PagedResp<AppListItemDto> list(Status status, String region, String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        var pageApps = repo.search(status, region, (q == null || q.isBlank()) ? null : q, pageable)
                .map(AppListItemDto::from);
        return PagedResp.of(pageApps);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public AppDetailDto detail(Long id) {
        HotelApplication a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return AppDetailDto.from(a);
    }

    /* -------------------- 심사 처리 -------------------- */

    public void approve(Long id, Long adminUserId, String note) {
        HotelApplication a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (a.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 심사 처리된 신청입니다.");
        }

        // 지역 정책: 제주 제외
        String region = a.getRegion() == null ? "" : a.getRegion().trim();
        if ("제주".equals(region) || "제주도".equals(region)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제주 지역은 등록할 수 없습니다.");
        }

        // canonical_key 보정
        if (a.getCanonicalKey() == null || a.getCanonicalKey().isBlank()) {
            a.setCanonicalKey(CanonicalKeyUtil.forHotel(a.getName(), a.getAddress()));
        }

        // hotels에 존재/생성
        Long hotelId = findOrCreateHotelFromApplication(a);

        // 상태 업데이트
        a.setStatus(Status.APPROVED);
        a.setReviewedBy(adminUserId);          // NULL 허용 컬럼이면 그대로 둠
        a.setReviewedAt(LocalDateTime.now());
        a.setRejectReason(null);
        repo.save(a);

        // 감사 로그
        auditRepo.save(HotelApplicationAudit.builder()
                .applicationId(a.getId())
                .adminUserId(adminUserId)
                .action(HotelApplicationAudit.Action.APPROVE)
                .note(((note == null) ? "" : note) + " (hotelId=" + hotelId + ")")
                .build());
    }

    public void reject(Long id, Long adminUserId, String reason, String note) {
        HotelApplication a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (a.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 심사 처리된 신청입니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 사유를 입력하세요.");
        }

        a.setStatus(Status.REJECTED);
        a.setRejectReason(reason);
        a.setReviewedBy(adminUserId);
        a.setReviewedAt(LocalDateTime.now());
        repo.save(a);

        auditRepo.save(HotelApplicationAudit.builder()
                .applicationId(a.getId())
                .adminUserId(adminUserId)
                .action(HotelApplicationAudit.Action.REJECT)
                .note(note)
                .build());
    }

    /* -------------------- hotels 연동 -------------------- */

    private Long findOrCreateHotelFromApplication(HotelApplication a) {
        String ck = a.getCanonicalKey();
        if (ck == null || ck.isBlank()) {
            throw new IllegalStateException("canonical_key is null (applicationId=" + a.getId() + ")");
        }

        Long existsId = selectHotelIdByCanonical(ck);
        if (existsId != null) return existsId;

        String coverType = (a.getCoverImageUrl() != null && !a.getCoverImageUrl().isBlank())
                ? "UPLOADED" : "NONE";

        // 유니크 충돌을 예외 없이 무해하게 처리
        em.createNativeQuery("""
            INSERT INTO hotels
              (name, region, address, phone, homepage_url, canonical_key,
               cover_image_type, cover_image_url, created_at, updated_at)
            VALUES
              (:name, :region, :address, :phone, :homepage, :ck,
               :ctype, :curl, NOW(), NOW())
            ON DUPLICATE KEY UPDATE id = id
        """)
          .setParameter("name", a.getName())
          .setParameter("region", a.getRegion())
          .setParameter("address", a.getAddress())
          .setParameter("phone", a.getPhone())
          .setParameter("homepage", a.getHomepageUrl())
          .setParameter("ck", ck)
          .setParameter("ctype", coverType)
          .setParameter("curl", a.getCoverImageUrl())
          .executeUpdate();

        Long id = selectHotelIdByCanonical(ck);
        if (id == null) throw new IllegalStateException("failed to create hotels row (ck=" + ck + ")");
        return id;
    }

    private Long selectHotelIdByCanonical(String ck) {
        var rows = em.createNativeQuery(
                        "SELECT id FROM hotels WHERE canonical_key = :ck LIMIT 1")
                .setParameter("ck", ck)
                .getResultList();
        return rows.isEmpty() ? null : ((Number) rows.get(0)).longValue();
    }
}
