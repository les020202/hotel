package com.example.hotelres.support;  
// 고객지원(공지사항, FAQ, 티켓 등) 관련 패키지

import com.example.hotelres.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name="notices")
// JPA 엔티티, 매핑 테이블 이름: notices
@Getter @Setter
// Lombok: getter/setter 자동 생성
public class Notice {

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  // 기본 키, AUTO_INCREMENT 전략

  private String title;
  // 공지 제목

  @Lob
  private String content;
  // 공지 내용 (큰 텍스트 저장 가능, CLOB)

  private boolean pinned;
  // 상단 고정 여부 (true면 게시판 최상단에 노출)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="author_id")
  private User author;
  // 작성자 (User 엔티티 참조), FK 컬럼명: author_id
  // 지연 로딩(LAZY) → 필요할 때만 User 조회

  @Column(name="created_at")
  private LocalDateTime createdAt;
  // 생성일시

  @Column(name="updated_at")
  private LocalDateTime updatedAt;
  // 수정일시
}
