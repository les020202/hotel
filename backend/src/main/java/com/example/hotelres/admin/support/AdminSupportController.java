package com.example.hotelres.admin.support;

import com.example.hotelres.admin.support.AdminSupportDtos.NoticeDto;
import com.example.hotelres.admin.support.FaqAdminDtos.FaqDto;
import com.example.hotelres.admin.support.TicketAdminDtos.MessageDto;
import com.example.hotelres.admin.support.TicketAdminDtos.TicketDetailDto;
import com.example.hotelres.admin.support.TicketAdminDtos.TicketRowDto;
import com.example.hotelres.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final AdminSupportManageService service;

    // -------- 공지 --------
    @GetMapping("/notices")
    public PageResponse<NoticeDto> listNotices(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean pinned
    ) {
        return service.listNotices(page, size, pinned);
    }

    @PostMapping("/notices")
    public Map<String, Long> createNotice(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        boolean pinned = body.get("pinned") != null && (Boolean) body.get("pinned");
        Long id = service.createNotice(title, content, pinned);
        return Map.of("id", id);
    }

    @PutMapping("/notices/{id}")
    public void updateNotice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean pinned = (body.get("pinned") == null) ? null : (Boolean) body.get("pinned");
        service.updateNotice(id, title, content, pinned);
    }

    @DeleteMapping("/notices/{id}")
    public void deleteNotice(@PathVariable Long id) {
        service.deleteNotice(id);
    }

    // -------- FAQ --------
    @GetMapping("/faqs")
    public PageResponse<FaqDto> listFaqs(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return service.listFaqs(category, page, size);
    }

    @PostMapping("/faqs")
    public Map<String, Long> createFaq(@RequestBody Map<String, Object> body) {
        String category = (String) body.get("category");
        String question = (String) body.get("question");
        String answer = (String) body.get("answer");
        Long id = service.createFaq(category, question, answer);
        return Map.of("id", id);
    }

    @PutMapping("/faqs/{id}")
    public void updateFaq(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String category = (String) body.get("category");
        String question = (String) body.get("question");
        String answer = (String) body.get("answer");
        service.updateFaq(id, category, question, answer);
    }

    @DeleteMapping("/faqs/{id}")
    public void deleteFaq(@PathVariable Long id) {
        service.deleteFaq(id);
    }

    // -------- 티켓/문의 --------
    @GetMapping("/tickets")
    public PageResponse<TicketRowDto> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return service.listTickets(status, q, page, size);
    }

    @GetMapping("/tickets/{id}")
    public TicketDetailDto getTicket(@PathVariable Long id) {
        return service.getTicket(id);
    }

    @DeleteMapping("/tickets/{id}")
    public void deleteTicket(@PathVariable Long id) {
        service.deleteTicket(id);
    }

    @PostMapping("/tickets/{id}/reply")
    public Map<String, Long> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long msgId = service.replyToTicket(id, body.get("content"));
        return Map.of("id", msgId);
    }

    @PutMapping("/messages/{msgId}")
    public void updateMessage(@PathVariable Long msgId, @RequestBody Map<String, String> body) {
        service.updateMessage(msgId, body.get("content"));
    }

    @DeleteMapping("/messages/{msgId}")
    public void deleteMessage(@PathVariable Long msgId) {
        service.deleteMessage(msgId);
    }
}
