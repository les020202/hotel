package com.example.hotelres.support;  
// 고객지원(상담 메시지) 관련 패키지

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MsgRepo extends JpaRepository<SupportMessage, Long> {
  // JpaRepository<엔티티, PK 타입> → 기본 CRUD, 페이징/정렬 메서드 자동 제공

  List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
  // 특정 티켓 ID에 속한 메시지들을 생성일시 오름차순(=대화 순서대로) 조회
}
