package com.example.hotelres.support;  
// 고객지원(티켓) 관련 패키지

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepo extends JpaRepository<SupportTicket, Long> {
  // JpaRepository<엔티티, PK 타입> → 기본 CRUD, 페이징/정렬 메서드 제공

  Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
  // 특정 사용자(userId)의 티켓을 생성일시 내림차순으로 페이징 조회
  // SQL: SELECT * FROM support_tickets WHERE user_id = ? ORDER BY created_at DESC
}
