package com.example.hotelres.api;  
// API 계층 컨트롤러 패키지

import com.example.hotelres.api.dto.SupportDtos.NoticeBrief;
import com.example.hotelres.api.dto.SupportDtos.NoticeDetail;
import com.example.hotelres.api.dto.SupportDtos.NoticeNavRes;
// 공지사항 API 응답용 DTO들

import com.example.hotelres.common.NotFoundException;
// 존재하지 않는 리소스를 조회했을 때 던지는 예외

import com.example.hotelres.support.Notice;
import com.example.hotelres.support.NoticeRepo;
// 공지사항 엔티티 및 JPA Repository

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
// 스프링 Web MVC 관련 import

@RestController
@RequestMapping("/api/notices")
// REST 컨트롤러, "/api/notices" 경로를 처리
@RequiredArgsConstructor
// 생성자 주입 자동 생성 (final 필드)
public class NoticeController {

    private final NoticeRepo repo; // 공지사항 JPA Repository

    /** 공지 목록 (고정글 → 최신글 순) */
    @GetMapping
    public Page<NoticeBrief> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        // 페이지 번호와 크기를 받아 공지사항 목록 조회
        Page<Notice> p = repo.findAllByOrderByPinnedDescIdDesc(PageRequest.of(page, size));
        // pinned=true(상단 고정) 우선, 그 다음 최신 id 순 정렬

        return p.map(n -> new NoticeBrief(
                n.getId(),        // 공지 ID
                n.getTitle(),     // 제목
                n.isPinned(),     // 상단 고정 여부
                n.getCreatedAt()  // 생성일시
        ));
    }
    // ▶ 공지사항 목록 API (공지사항 게시판 리스트에 사용)

    /** 공지 상세 */
    @GetMapping("/{id}")
    public NoticeDetail detail(@PathVariable Long id) {
        // ID로 공지사항 조회 (없으면 NotFoundException)
        Notice n = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("notice not found"));

        return new NoticeDetail(
                n.getId(),        // 공지 ID
                n.getTitle(),     // 제목
                n.getContent(),   // 내용
                n.isPinned(),     // 상단 고정 여부
                n.getCreatedAt()  // 생성일시
        );
    }
    // ▶ 공지사항 상세보기 API (상세 페이지에서 사용)

    /** 이전글 / 다음글 조회 */
    @GetMapping("/{id}/nav")
    public NoticeNavRes nav(@PathVariable Long id) {
        // 이전글: 현재 글보다 id 큰 것 중 가장 작은 id (리스트에서 위쪽 글)
        Long prevId = repo.findFirstByIdGreaterThanOrderByIdAsc(id)
            .map(Notice::getId)
            .orElse(null);

        // 다음글: 현재 글보다 id 작은 것 중 가장 큰 id (리스트에서 아래쪽 글)
        Long nextId = repo.findFirstByIdLessThanOrderByIdDesc(id)
            .map(Notice::getId)
            .orElse(null);

        return new NoticeNavRes(prevId, nextId);
    }
    // ▶ 공지사항 상세 페이지에서 "이전글/다음글" 이동 버튼 기능 제공
}
