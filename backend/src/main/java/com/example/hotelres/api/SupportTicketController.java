package com.example.hotelres.api;  
// API 계층 컨트롤러 패키지

import com.example.hotelres.api.dto.SupportDtos.MessageReq;
import com.example.hotelres.api.dto.SupportDtos.MessageRes;
import com.example.hotelres.api.dto.SupportDtos.TicketBrief;
import com.example.hotelres.api.dto.SupportDtos.TicketNewReq;
// 고객지원(티켓/메시지) 관련 DTO들

import com.example.hotelres.common.CurrentUser;
import com.example.hotelres.common.NotFoundException;
// 현재 로그인 사용자 조회, 리소스 없음 예외

import com.example.hotelres.support.*;
import com.example.hotelres.user.User;
// 지원 티켓, 메시지 엔티티와 레포지토리, User 엔티티

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
// Spring Web, 페이징, 인증 관련 import

import java.time.LocalDateTime;
import java.util.List;
// 시간 처리, 리스트

@RestController
@RequestMapping("/api/support")
// REST 컨트롤러, "/api/support" 경로 처리
@RequiredArgsConstructor
// final 필드 자동 주입
public class SupportTicketController {
  private final CurrentUser currentUser; // 현재 로그인 사용자 가져오기
  private final TicketRepo tickets;      // 상담 티켓 저장/조회 Repo
  private final MsgRepo msgs;            // 상담 메시지 저장/조회 Repo

  // ===== 내 티켓 목록 조회 =====
  @GetMapping("/tickets")
  public Page<TicketBrief> myTickets(Authentication auth,
                                     @RequestParam(defaultValue="0") int page,
                                     @RequestParam(defaultValue="10") int size) {
    User me = currentUser.get(auth); // 로그인 사용자
    return tickets.findByUserIdOrderByCreatedAtDesc(me.getId(), PageRequest.of(page, size))
        .map(t -> new TicketBrief(
            t.getId(),             // 티켓 ID
            t.getSubject(),        // 제목
            t.getStatus().name(),  // 상태 (예: OPEN, CLOSED)
            t.getCreatedAt()       // 생성일시
        ));
  }
  // ▶ 내가 작성한 상담 티켓 목록을 페이징으로 조회하는 API

  // ===== 새 티켓 열기 =====
  @PostMapping("/tickets")
  public TicketBrief open(Authentication auth, @RequestBody TicketNewReq req) {
    User me = currentUser.get(auth);
    SupportTicket t = new SupportTicket();
    t.setUser(me);                      // 작성자
    t.setSubject(req.subject());        // 제목
    t.setStatus(SupportTicket.Status.UPLOADED); // 상태 (기본값)
    t.setCreatedAt(LocalDateTime.now());
    t.setUpdatedAt(LocalDateTime.now());
    t = tickets.save(t);                // DB 저장

    // 첫 메시지가 있을 경우 함께 저장
    if (req.firstMessage()!=null && !req.firstMessage().isBlank()) {
      SupportMessage m = new SupportMessage();
      m.setTicket(t); 
      m.setSender(me); 
      m.setContent(req.firstMessage());
      m.setCreatedAt(LocalDateTime.now());
      msgs.save(m);
    }

    return new TicketBrief(
        t.getId(), t.getSubject(), t.getStatus().name(), t.getCreatedAt()
    );
  }
  // ▶ 새 상담 티켓 생성 API (최초 메시지까지 같이 저장 가능)

  // ===== 티켓의 메시지 목록 조회 =====
  @GetMapping("/tickets/{id}/messages")
  public List<MessageRes> messages(Authentication auth, @PathVariable Long id) {
    // (옵션) 권한 체크: 본인 티켓만 조회 가능하도록 하면 안전
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));

    return msgs.findByTicketIdOrderByCreatedAtAsc(id)
      .stream()
      .map(m -> new MessageRes(
          m.getId(),                  // 메시지 ID
          m.getSender().getId(),      // 보낸 사람 ID
          m.getContent(),             // 내용
          m.getCreatedAt()            // 작성일시
      ))
      .toList();
  }
  // ▶ 특정 티켓의 메시지 내역(대화 내용) 조회 API

  // ===== 티켓에 메시지 추가 =====
  @PostMapping("/tickets/{id}/messages")
  public void addMessage(Authentication auth, @PathVariable Long id, @RequestBody MessageReq req) {
    User me = currentUser.get(auth);
    SupportTicket t = tickets.findById(id)
        .orElseThrow(() -> new NotFoundException("ticket not found"));

    SupportMessage m = new SupportMessage();
    m.setTicket(t); 
    m.setSender(me); 
    m.setContent(req.content());
    m.setCreatedAt(LocalDateTime.now());
    msgs.save(m);
  }
  // ▶ 특정 티켓에 새 메시지를 추가하는 API (문의 추가/답변)
}
