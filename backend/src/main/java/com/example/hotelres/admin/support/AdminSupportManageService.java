package com.example.hotelres.admin.support;

import com.example.hotelres.admin.support.AdminSupportDtos.NoticeDto;
import com.example.hotelres.admin.support.FaqAdminDtos.FaqDto;
import com.example.hotelres.admin.support.TicketAdminDtos.MessageDto;
import com.example.hotelres.admin.support.TicketAdminDtos.TicketDetailDto;
import com.example.hotelres.admin.support.TicketAdminDtos.TicketRowDto;
import com.example.hotelres.common.dto.PageResponse;
import com.example.hotelres.support.Faq;
import com.example.hotelres.support.FaqRepo;
import com.example.hotelres.support.Notice;
import com.example.hotelres.support.NoticeRepo;
import com.example.hotelres.support.SupportMessage;
import com.example.hotelres.support.SupportTicket;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSupportManageService {

    private final NoticeRepo noticeRepo;
    private final FaqRepo faqRepo;
    private final AdminTicketRepo ticketRepo;   // 티켓
    private final AdminMsgRepo    msgRepo;      // 메시지
    private final UserRepository userRepository;

    // ---------- 공통 ----------
    private Pageable page(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
    }
    private Pageable page(int page, int size) {
        return page(page, size, Sort.unsorted());
    }
    private static <T> PageResponse<T> toPage(Page<T> p) {
        return PageResponse.from(p);
    }
    private User currentUserOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        String login = a.getName();
        return userRepository.findByLoginId(login).orElse(null);
    }

    // ============== 공지 ==============
    @Transactional(readOnly = true)
    public PageResponse<NoticeDto> listNotices(Integer page, Integer size, Boolean pinned) {
        Pageable pageable = page(
                page == null ? 0 : page,
                size == null ? 20 : size,
                Sort.by(Order.desc("pinned"), Order.desc("id"))
        );

        Page<Notice> p = (pinned == null)
                ? noticeRepo.findAll(pageable)
                : noticeRepo.findAll((root, q, cb) -> cb.equal(root.get("pinned"), pinned), pageable);

        return toPage(p.map(NoticeDto::from));
    }

    @Transactional
    public Long createNotice(String title, String content, boolean pinned) {
        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setPinned(pinned);
        n.setAuthor(currentUserOrNull());
        n.setCreatedAt(LocalDateTime.now());
        n.setUpdatedAt(LocalDateTime.now());
        noticeRepo.save(n);
        return n.getId();
    }

    @Transactional
    public void updateNotice(Long id, String title, String content, Boolean pinned) {
        Notice n = noticeRepo.findById(id).orElseThrow();
        if (title != null) n.setTitle(title);
        if (content != null) n.setContent(content);
        if (pinned != null) n.setPinned(pinned);
        n.setUpdatedAt(LocalDateTime.now());
        noticeRepo.save(n);
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (noticeRepo.existsById(id)) noticeRepo.deleteById(id);
    }

    // ============== FAQ ==============
    @Transactional(readOnly = true)
    public PageResponse<FaqDto> listFaqs(String category, int page, int size) {
        Pageable pageable = page(page, size, Sort.by(Order.desc("id")));
        Page<Faq> p;

        if (category == null || category.isBlank()) {
            p = faqRepo.findAll(pageable);
        } else {
            Faq probe = new Faq();
            probe.setCategory(category);
            ExampleMatcher matcher = ExampleMatcher.matchingAll()
                    .withIgnoreNullValues()
                    .withMatcher("category", ExampleMatcher.GenericPropertyMatchers.exact().ignoreCase());
            p = faqRepo.findAll(Example.of(probe, matcher), pageable);
        }

        return toPage(p.map(FaqDto::from));
    }

    @Transactional
    public Long createFaq(String category, String question, String answer) {
        Faq f = new Faq();
        f.setCategory(category);
        f.setQuestion(question);
        f.setAnswer(answer);
        User me = currentUserOrNull();
        f.setAuthorId(me != null ? me.getId() : null);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        faqRepo.save(f);
        return f.getId();
    }

    @Transactional
    public void updateFaq(Long id, String category, String question, String answer) {
        Faq f = faqRepo.findById(id).orElseThrow();
        if (category != null) f.setCategory(category);
        if (question != null) f.setQuestion(question);
        if (answer != null) f.setAnswer(answer);
        f.setUpdatedAt(LocalDateTime.now());
        faqRepo.save(f);
    }

    @Transactional
    public void deleteFaq(Long id) {
        if (faqRepo.existsById(id)) faqRepo.deleteById(id);
    }

    // ============== 티켓/문의 ==============
    @Transactional(readOnly = true)
    public PageResponse<TicketRowDto> listTickets(String status, String q, int page, int size) {
        SupportTicket.Status st = null;
        if (status != null && !status.isBlank()) {
            st = SupportTicket.Status.valueOf(status.toUpperCase());
        }
        Page<SupportTicket> p = ticketRepo.search(st, q, page(page, size, Sort.by(Order.desc("createdAt"))));
        return toPage(p.map(TicketRowDto::from));
    }

    @Transactional(readOnly = true)
    public TicketDetailDto getTicket(Long id) {
        SupportTicket t = ticketRepo.findById(id).orElseThrow();
        List<SupportMessage> msgs = msgRepo.findByTicketIdOrderByCreatedAtAsc(id);
        return TicketDetailDto.from(t, msgs);
    }

    @Transactional
    public void deleteTicket(Long id) {
        if (ticketRepo.existsById(id)) ticketRepo.deleteById(id); // 메시지는 FK CASCADE 또는 orphanRemoval=true
    }

    @Transactional
    public Long replyToTicket(Long ticketId, String content) {
        SupportTicket t = ticketRepo.findById(ticketId).orElseThrow();
        User admin = currentUserOrNull();

        SupportMessage m = new SupportMessage();
        m.setTicket(t);
        m.setSender(admin);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        msgRepo.save(m);

        t.setStatus(SupportTicket.Status.ANSWERED);
        t.setUpdatedAt(LocalDateTime.now());
        ticketRepo.save(t);

        return m.getId();
    }

    @Transactional
    public void updateMessage(Long msgId, String content) {
        SupportMessage m = msgRepo.findById(msgId).orElseThrow();
        m.setContent(content);
        msgRepo.save(m);
    }

    @Transactional
    public void deleteMessage(Long msgId) {
        if (msgRepo.existsById(msgId)) msgRepo.deleteById(msgId);
    }
}
