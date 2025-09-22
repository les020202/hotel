package com.example.hotelres.support;  
// 고객지원(FAQ, 공지사항, 티켓 등) 관련 패키지

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Entity 
@Table(name="faqs")
// JPA 엔티티, 매핑 테이블 이름: faqs
@Getter @Setter
// Lombok: getter/setter 자동 생성
public class Faq {

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;  
  // 기본 키, AUTO_INCREMENT 전략

  @Column(nullable=false, length=50)
  private String category; 
  // FAQ 분류 (예: 숙소, 결제, 회원, 쿠폰 등)

  @Column(nullable=false, length=200)
  private String question; 
  // FAQ 질문 (200자 제한)

  @Lob 
  @Column(nullable=false)
  private String answer; 
  // FAQ 답변 (큰 텍스트 저장 가능, CLOB)

  private Long authorId; 
  // 작성자 ID (User 엔티티와 직접 연관관계 매핑 대신 단순 ID로 관리)

  private LocalDateTime createdAt; 
  // 생성일시
  private LocalDateTime updatedAt; 
  // 수정일시
}
