package com.example.hotelres.support;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;


/*
 * TicketRepo
 * - SupportTicket 엔티티에 대한 JPA 리포지토리 인터페이스
 * - JpaRepository<SupportTicket, Long>을 상속하여 기본 CRUD 메서드 제공
 */
public interface TicketRepo extends JpaRepository<SupportTicket, Long> {

  // 사용자 ID 기준, 생성일 내림차순으로 페이지네이션 목록 조회
  // 메서드 이름 규칙을 사용한 쿼리 생성 (Spring Data JPA)
  Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  // 상태(status)와 키워드(q: 제목 like)로 필터링하는 검색 쿼리
  // - :status 가 null이면 상태 조건을 건너뜀
  // - :q 가 null 또는 빈 문자열이면 제목 검색을 건너뜀
  // - lower(...) 로 대소문자 구분 없이 like 검색

  @Query("""
     select t from SupportTicket t
      where t.user.id = :userId
        and (:status is null or t.status = :status)
        and (:q is null or :q = '' or lower(t.subject) like lower(concat('%', :q, '%')))
      order by t.createdAt desc
  """)

  Page<SupportTicket> searchMy(@Param("userId") Long userId,            // 조회 대상 사용자 ID
                               @Param("status") SupportTicket.Status status, // 필터용 상태 값(옵션)
                               @Param("q") String q,                        // 제목 키워드(옵션)
                               Pageable pageable);                          // 페이지네이션/정렬 정보
}
