package com.example.hotelres.api;

import com.example.hotelres.api.dto.SupportDtos;
import com.example.hotelres.api.dto.SupportDtos.*;
import com.example.hotelres.common.CurrentUser;
import com.example.hotelres.common.NotFoundException;
import com.example.hotelres.support.*;
import com.example.hotelres.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {
  private final CurrentUser currentUser;
  private final TicketRepo tickets;
  private final MsgRepo msgs;

  // ─────────────────────────────────────────────────────────────────────
  // 1) 내 티켓 목록 + 상태/검색어 필터
  @GetMapping("/tickets")
  public TicketsRes myTickets(Authentication auth,
                              @RequestParam(defaultValue="0") int page,
                              @RequestParam(defaultValue="10") int size,
                              @RequestParam(required=false) String status,   // UPLOADED | ANSWERED
                              @RequestParam(name="q", required=false) String keyword) {
    User me = currentUser.get(auth);
    SupportTicket.Status st = null;
    if (status != null && !status.isBlank()) {
      st = SupportTicket.Status.valueOf(status);
    }
    Page<SupportTicket> p = tickets.searchMy(me.getId(), st, keyword, PageRequest.of(page, size));
    return new TicketsRes(p.map(SupportDtos.TicketBrief::of).getContent());
  }

  // 2) 새 티켓 생성 (최초 메시지 포함 가능)
  @PostMapping("/tickets")
  public TicketBrief open(Authentication auth, @RequestBody TicketNewReq req) {
    User me = currentUser.get(auth);

    SupportTicket t = new SupportTicket();
    t.setUser(me);
    t.setSubject(req.subject());
    t.setStatus(SupportTicket.Status.UPLOADED);
    t.setCreatedAt(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t = tickets.save(t);

    if (req.firstMessage() != null && !req.firstMessage().isBlank()) {
      SupportMessage m = new SupportMessage();
      m.setTicket(t);
      m.setSender(me);
      m.setContent(req.firstMessage());
      m.setCreatedAt(LocalDateTime.now());
      msgs.save(m);
    }

    return SupportDtos.TicketBrief.of(t);
  }

  // 3) 티켓 단건 조회 (본인만, 관리자면 허용)
  @GetMapping("/tickets/{id}")
  public TicketBrief ticket(Authentication auth, @PathVariable Long id) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));
    //if (!isOwnerOrAdmin(me, t)) throw new NotFoundException("ticket not found");
    return SupportDtos.TicketBrief.of(t);
  }

  // 4) 티켓 메시지 목록 (본인만)
  @GetMapping("/tickets/{id}/messages")
  public List<MessageRes> messages(Authentication auth, @PathVariable Long id) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));
    //if (!isOwnerOrAdmin(me, t)) throw new NotFoundException("ticket not found");

    return msgs.findByTicketIdOrderByCreatedAtAsc(id).stream()
        .map(m -> new MessageRes(m.getId(), m.getSender().getId(), m.getContent(), m.getCreatedAt()))
        .toList();
  }

  // 5) 티켓에 메시지 추가 (본인만)
  @PostMapping("/tickets/{id}/messages")
  public void addMessage(Authentication auth, @PathVariable Long id, @RequestBody MessageReq req) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));
    //if (!isOwnerOrAdmin(me, t)) throw new NotFoundException("ticket not found");

    SupportMessage m = new SupportMessage();
    m.setTicket(t);
    m.setSender(me);
    m.setContent(req.content());
    m.setCreatedAt(LocalDateTime.now());
    msgs.save(m);
  }
  /* 
  private boolean isOwnerOrAdmin(User me, SupportTicket t) {
    // 관리자 판별 로직은 프로젝트 권한체계에 맞춰 수정
    
    boolean admin = me.getRoles() != null && me.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
    return admin || t.getUser().getId().equals(me.getId());
   
  }
  */
}
