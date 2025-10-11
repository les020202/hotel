package com.example.hotelres.admin.support;

import com.example.hotelres.support.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminTicketRepo extends JpaRepository<SupportTicket, Long> {

    @Query("""
       select t from SupportTicket t
       where (:status is null or t.status = :status)
         and (:q is null or :q = '' or lower(t.subject) like lower(concat('%', :q, '%')))
       order by t.createdAt desc
    """)
    Page<SupportTicket> search(@Param("status") SupportTicket.Status status,
                               @Param("q") String q,
                               Pageable pageable);
}
