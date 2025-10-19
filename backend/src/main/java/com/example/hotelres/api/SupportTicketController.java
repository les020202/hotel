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

// ★ XSS 정화용
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {
  private final CurrentUser currentUser;
  private final TicketRepo tickets;
  private final MsgRepo msgs;

  // ====== 공통 정화/검증 유틸 ======
  /** HTML 완전 제거 + 앞뒤 공백 제거 + 빈문자열은 null로 */
  private static String cleanText(String s) {
    if (s == null) return null;
    String v = Jsoup.clean(s, Safelist.none()); // 태그/이벤트 싹 제거
    v = v.replace("\u0000", "").trim();         // 널문자/공백 제거
    return v.isEmpty() ? null : v;
  }
  /** 길이 제한(한글 안전하게 substring) */
  private static String clamp(String s, int max) {
    if (s == null) return null;
    return (s.length() <= max) ? s : s.substring(0, max);
  }
  private static String sanitizeSubject(String s) {
    return clamp(cleanText(s), 150);
  }
  private static String sanitizeBody(String s) {
    return clamp(cleanText(s), 8000); // 내용은 넉넉히
  }
  // =================================

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

    // ✅ XSS 정화 + 길이 제한
    String subject = sanitizeSubject(req.subject());
    String first   = sanitizeBody(req.firstMessage());

    if (subject == null) {
      throw new IllegalArgumentException("제목을 입력해주세요.");
    }

    SupportTicket t = new SupportTicket();
    t.setUser(me);
    t.setSubject(subject);
    t.setStatus(SupportTicket.Status.UPLOADED);
    t.setCreatedAt(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t = tickets.save(t);

    if (first != null) {
      SupportMessage m = new SupportMessage();
      m.setTicket(t);
      m.setSender(me);
      m.setContent(first);
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
              ? sender.getRole().name()
              : null;

          return new MessageRes(
              m.getId(),
              sender != null ? sender.getId() : null,
              senderRole,
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

    // ✅ XSS 정화 + 길이 제한 + 빈문자열 방지
    String content = sanitizeBody(req.content());
    if (content == null) {
      throw new IllegalArgumentException("내용을 입력해주세요.");
    }

    SupportMessage m = new SupportMessage();
    m.setTicket(t);
    m.setSender(me);
    m.setContent(content);
    m.setCreatedAt(LocalDateTime.now());
    msgs.save(m);
  }

  /** 단일 Enum role 기반 + Security 권한 기반(둘 중 하나라도 ADMIN이면 true) */
  private boolean isOwnerOrAdmin(User me, SupportTicket t, Authentication auth) {
    boolean isOwner = t.getUser() != null && t.getUser().getId() != null && t.getUser().getId().equals(me.getId());
    boolean isAdminByUser = me.getRole() == User.Role.ROLE_ADMIN;
    boolean isAdminByAuth = auth != null && auth.getAuthorities() != null &&
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    return isOwner || isAdminByUser || isAdminByAuth;
  }
}
