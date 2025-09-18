// src/main/java/com/example/hotelres/wishlist/Wishlist.java
package com.example.hotelres.wishlist;

import com.example.hotelres.hotel.Hotel;       // ✅ DTO 아님! 실제 Hotel 엔티티를 참조
import com.example.hotelres.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity // JPA 엔티티로 선언 → DB 테이블과 매핑됨
@Table(
    name = "wishlists",
    uniqueConstraints = @UniqueConstraint(
        name="uk_wishlist_user_hotel", columnNames = {"user_id","hotel_id"})) // 동일 사용자-호텔 조합 중복 방지(유니크 키)
public class Wishlist {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 기본 키(자동 증가)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user; // 위시리스트 소유자(사용자). FK: user_id, 지연 로딩

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hotel_id", nullable = false)
  private Hotel hotel;     // ✅ 엔티티 참조: 담아둔 호텔 객체. FK: hotel_id, 지연 로딩

  @Column(name="created_at", nullable=false)
  private LocalDateTime createdAt = LocalDateTime.now(); // 담긴 시각(기본값: 현재 시각)

  // getter/setter …
}
