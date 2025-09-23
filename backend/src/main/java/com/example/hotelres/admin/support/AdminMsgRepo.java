package com.example.hotelres.admin.support;

import com.example.hotelres.support.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMsgRepo extends JpaRepository<SupportMessage, Long> {

    @Query("""
       select m from SupportMessage m
       where m.ticket.id = :ticketId
       order by m.createdAt asc
    """)
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") Long ticketId);
}
