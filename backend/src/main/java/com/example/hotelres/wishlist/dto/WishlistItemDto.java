// src/main/java/com/example/hotelres/wishlist/dto/WishlistItemDto.java
package com.example.hotelres.wishlist.dto;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import lombok.Data;
import java.time.LocalDateTime;

@Data // Lombok: 모든 필드에 대한 getter/setter, toString, equals/hashCode 생성
public class WishlistItemDto {
  private Long wishlistId;                 // 위시리스트 항목의 고유 ID
  private LocalDateTime createdAt;         // 항목이 생성(담김)된 시각
  private HotelDetailsDto.Hotel hotel;     // 호텔 요약 정보 DTO(응답용 서머리)
}
