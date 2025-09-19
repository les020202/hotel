// src/main/java/com/example/hotelres/wishlist/dto/WishlistPageDto.java
package com.example.hotelres.wishlist.dto;

import lombok.Data;
import java.util.List;

@Data // Lombok: getter/setter, toString, equals/hashCode 자동 생성
public class WishlistPageDto {
  private List<WishlistItemDto> items; // 현재 페이지에 포함된 위시리스트 항목들
  private long total;                   // 전체 위시리스트 항목 개수(페이징 전체 기준)
  private boolean hasMore;              // 다음 페이지가 더 있는지 여부
  private int nextOffset;               // 다음 페이지 조회를 위한 오프셋 값
}
