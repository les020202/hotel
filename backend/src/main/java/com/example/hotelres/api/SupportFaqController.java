package com.example.hotelres.api;  
// API 계층 컨트롤러 패키지

import com.example.hotelres.api.dto.SupportDtos.FaqItem;
// FAQ 응답 DTO (id, category, question, answer)

import com.example.hotelres.common.NotFoundException;
// 리소스가 없을 때 발생하는 예외

import com.example.hotelres.support.Faq;
import com.example.hotelres.support.FaqRepo;
// FAQ 엔티티 및 JPA Repository

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
// Spring Web MVC 관련 import

@RestController
@RequestMapping("/api/support")
// REST 컨트롤러, "/api/support" 경로 처리
@RequiredArgsConstructor
// 생성자 자동 주입
public class SupportFaqController {

  private final FaqRepo faqs; // FAQ 데이터 접근용 JPA Repo

  // 목록: /api/support/faq?category=숙소&page=0&size=10
  @GetMapping("/faq")
  public Page<FaqItem> list(@RequestParam(required=false) String category,
                            @RequestParam(defaultValue="0") int page,
                            @RequestParam(defaultValue="10") int size) {
    // category: FAQ 분류(숙소, 쿠폰, 결제 등)
    // page/size: 페이징 처리 (기본 0페이지, 10개)

    var pageable = PageRequest.of(page, size); // 페이징 정보 객체 생성

    Page<Faq> p;
    if (category == null || category.isBlank() || "전체".equals(category)) {
      // 카테고리 없거나 "전체" 선택 시 → 전체 FAQ 조회
      p = faqs.findAll(pageable);
    } else {
      // 카테고리 있으면 → 대소문자 무시하고 해당 분류만 조회
      p = faqs.findByCategoryIgnoreCase(category, pageable);
    }

    // Faq 엔티티 → FaqItem DTO 변환
    return p.map(f -> new FaqItem(
        f.getId(),        // FAQ ID
        f.getCategory(),  // 분류
        f.getQuestion(),  // 질문
        f.getAnswer()     // 답변
    ));
  }
  // ▶ FAQ 목록 API (필터, 페이징 지원)

  // 상세: /api/support/faq/{id}
  @GetMapping("/faq/{id}")
  public FaqItem detail(@PathVariable Long id) {
    // ID로 FAQ 조회 (없으면 NotFoundException 발생)
    Faq f = faqs.findById(id).orElseThrow(() -> new NotFoundException("FAQ not found"));

    return new FaqItem(
        f.getId(),        // FAQ ID
        f.getCategory(),  // 분류
        f.getQuestion(),  // 질문
        f.getAnswer()     // 답변
    );
  }
  // ▶ FAQ 상세보기 API
}
