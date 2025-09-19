package com.example.hotelres.support;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TicketRepo extends JpaRepository<SupportTicket, Long> {

  // 기존 간단 목록
  Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  // 상태 / 키워드(제목 like) 필터 검색
  @Query("""
     select t from SupportTicket t
      where t.user.id = :userId
        and (:status is null or t.status = :status)
        and (:q is null or :q = '' or lower(t.subject) like lower(concat('%', :q, '%')))
      order by t.createdAt desc
  """)
  Page<SupportTicket> searchMy(@Param("userId") Long userId,
                               @Param("status") SupportTicket.Status status,
                               @Param("q") String q,
                               Pageable pageable);
}
