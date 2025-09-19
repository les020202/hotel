package com.example.hotelres.support;  
// 고객지원(공지사항, FAQ, 티켓 등) 관련 패키지

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepo extends JpaRepository<Notice, Long> {
    // JpaRepository<엔티티, PK 타입> → CRUD, 페이징, 정렬 기능 기본 제공

    /** 공지 목록 조회: pinned 우선 + 최신글(id DESC) */
    Page<Notice> findAllByOrderByPinnedDescIdDesc(Pageable pageable);
    // 상단 고정(pinned=true) 먼저 정렬, 그다음 id 내림차순으로 공지 목록 반환

    // 현재 글(id)보다 작은 최신 글 → 이전글
    Optional<Notice> findFirstByIdLessThanOrderByIdDesc(Long id);
    // id < 현재글 → 가장 큰 id (바로 이전 글)

    // 현재 글(id)보다 큰 가장 오래된 글 → 다음글
    Optional<Notice> findFirstByIdGreaterThanOrderByIdAsc(Long id);
    // id > 현재글 → 가장 작은 id (바로 다음 글)
}
