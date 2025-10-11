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

  // 3) 티켓 단건 조회 (본인 or 관리자)
  @GetMapping("/tickets/{id}")
  public TicketBrief ticket(Authentication auth, @PathVariable Long id) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));
    if (!isOwnerOrAdmin(me, t, auth)) throw new NotFoundException("ticket not found");
    return SupportDtos.TicketBrief.of(t);
  }

  // 4) 티켓 메시지 목록 (본인 or 관리자)
 @GetMapping("/tickets/{id}/messages")
public List<MessageRes> messages(Authentication auth, @PathVariable Long id) {
  User me = currentUser.get(auth);
  SupportTicket t = tickets.findById(id)
      .orElseThrow(() -> new NotFoundException("ticket not found"));
  // 권한체크를 살릴 거면 아래 주석 해제
  // if (!isOwnerOrAdmin(me, t, auth)) throw new NotFoundException("ticket not found");

  return msgs.findByTicketIdOrderByCreatedAtAsc(id).stream()
      .map(m -> {
        User sender = m.getSender();
        String senderRole = (sender != null && sender.getRole() != null)
            ? sender.getRole().name()   // 예: ROLE_ADMIN, ROLE_USER
            : null;

        return new MessageRes(
            m.getId(),
            sender != null ? sender.getId() : null,
            senderRole,                // ✅ 추가 필드
            m.getContent(),
            m.getCreatedAt()
        );
      })
      .toList();
}



  // 5) 티켓에 메시지 추가 (본인 or 관리자)
  @PostMapping("/tickets/{id}/messages")
  public void addMessage(Authentication auth, @PathVariable Long id, @RequestBody MessageReq req) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));
    if (!isOwnerOrAdmin(me, t, auth)) throw new NotFoundException("ticket not found");

    SupportMessage m = new SupportMessage();
    m.setTicket(t);
    m.setSender(me);
    m.setContent(req.content());
    m.setCreatedAt(LocalDateTime.now());
    msgs.save(m);
  }

  /** 단일 Enum role 기반 + Security 권한 기반(둘 중 하나라도 ADMIN이면 true) */
  private boolean isOwnerOrAdmin(User me, SupportTicket t, Authentication auth) {
    boolean isOwner = t.getUser() != null && t.getUser().getId() != null && t.getUser().getId().equals(me.getId());

    // User 엔티티의 단일 Enum 필드로 체크
    boolean isAdminByUser = me.getRole() == User.Role.ROLE_ADMIN;

    // Spring Security 권한으로도 보조 체크 (ex. DB와 상이할 수 있는 경우)
    boolean isAdminByAuth = auth != null && auth.getAuthorities() != null &&
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

    return isOwner || isAdminByUser || isAdminByAuth /* || isOwnerRole */;
  }
}
