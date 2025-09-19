package com.example.hotelres.api.dto;  
// 고객지원(Support) 관련 DTO들을 모아둔 패키지

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
// 날짜/시간을 다루기 위해 LocalDateTime 사용

public class SupportDtos {
    // 고객지원 기능(공지사항, FAQ, 상담 티켓 등)에 사용하는 DTO들을 모은 클래스

    // ── Notices (공지사항 관련)
    public record NoticeBrief(
        Long id,                 // 공지사항 ID
        String title,            // 제목
        boolean pinned,          // 상단 고정 여부
        LocalDateTime createdAt  // 생성일시
    ) {}
    // ▶ NoticeBrief : 공지사항 목록에 보여줄 간단 정보 DTO

    public record NoticeDetail(
        Long id,                 // 공지사항 ID
        String title,            // 제목
        String content,          // 상세 내용
        boolean pinned,          // 상단 고정 여부
        LocalDateTime createdAt  // 생성일시
    ) {}
    // ▶ NoticeDetail : 공지사항 상세보기 DTO

    public record NoticeNavRes(
        Long prevId,  // 이전 글 ID (없으면 null)
        Long nextId   // 다음 글 ID (없으면 null)
    ) {}
    // ▶ NoticeNavRes : 공지사항 상세보기에서 "이전/다음 글 이동"을 위한 DTO

    // ── FAQs (자주 묻는 질문 관련)
    public record FaqItem(
        Long id,         // FAQ ID
        String category, // 분류 (예: 숙소, 쿠폰, 결제 등)
        String question, // 질문
        String answer    // 답변
    ) {}
    // ▶ FaqItem : FAQ 한 건을 표현하는 DTO

    // ── Tickets & Messages (상담 티켓 및 메시지 관련)
    public record TicketNewReq(
        String subject,       // 티켓 제목
        String firstMessage   // 최초 문의 내용
    ) {}
    // ▶ TicketNewReq : 새 상담 티켓 생성 요청 DTO

    public record TicketBrief(
        Long id,                 // 티켓 ID
        String subject,          // 제목
        String status,           // 상태 (예: OPEN, CLOSED 등)
        LocalDateTime createdAt  // 생성일시
    ) {}
    // ▶ TicketBrief : 상담 티켓 목록에서 간단 정보 DTO

    public record MessageReq(
        String content  // 메시지 내용
    ) {}
    // ▶ MessageReq : 티켓 내에 메시지를 추가할 때 사용하는 요청 DTO

    public record MessageRes(
        Long id,                 // 메시지 ID
        Long senderId,           // 보낸 사람 ID
        String content,          // 메시지 내용
        LocalDateTime createdAt  // 작성일시
    ) {}
    // ▶ MessageRes : 티켓 내 메시지를 클라이언트로 내려줄 때 사용하는 응답 DTO
}
