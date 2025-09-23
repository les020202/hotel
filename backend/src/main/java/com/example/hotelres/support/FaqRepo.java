package com.example.hotelres.support;  
// 고객지원(FAQ) 관련 패키지

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepo extends JpaRepository<Faq, Long> {
  // JpaRepository<Faq, Long> → Faq 엔티티 기본 CRUD, 페이징/정렬 기능 제공

  Page<Faq> findByCategoryIgnoreCase(String category, Pageable pageable);
  // 카테고리를 대소문자 구분 없이 검색 + 페이징 처리
  // SELECT * FROM faqs WHERE LOWER(category) = LOWER(:category)
}
