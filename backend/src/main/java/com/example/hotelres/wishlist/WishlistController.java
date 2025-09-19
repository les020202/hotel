// src/main/java/com/example/hotelres/wishlist/WishlistController.java
package com.example.hotelres.wishlist;

import com.example.hotelres.hotel.dto.HotelDetailsDto;
import com.example.hotelres.user.UserRepository;  // ✅ 추가
import com.example.hotelres.wishlist.dto.WishlistItemDto;
import com.example.hotelres.wishlist.dto.WishlistPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestController // REST 컨트롤러: 반환값을 JSON으로 응답
@RequestMapping("/api/wishlists") // 이 컨트롤러의 기본 URL 프리픽스
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성(생성자 주입)
public class WishlistController {

    private final WishlistRepository repo;         // 위시리스트 데이터 접근 리포지토리
    private final UserRepository userRepository;   // ✅ 로그인 아이디 → 사용자 ID 조회용

    @GetMapping // GET /api/wishlists
    public WishlistPageDto list(
            @AuthenticationPrincipal(expression = "username") String loginId, // ✅ 인증 주체의 username(=loginId) 추출
            @RequestParam(defaultValue = "0") int offset,  // 페이지 오프셋(기본 0)
            @RequestParam(defaultValue = "4") int limit    // 페이지 크기(기본 4)
    ) {
        // username(loginId) → 사용자 ID로 변환. 없으면 401 반환
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        // offset/limit → PageRequest(page, size) 계산
        int page = offset / limit;
        var p = repo.findRowsByUserId(userId, PageRequest.of(page, limit)); // 페이징 결과 조회

        // 페이지 결과를 WishlistItemDto 목록으로 매핑
        var items = p.getContent().stream().map(r -> {
            var dto = new WishlistItemDto();
            dto.setWishlistId(r.getWishlistId());   // 항목 ID
            dto.setCreatedAt(r.getCreatedAt());     // 담긴 시각

            // 호텔 요약 DTO 구성(서버 응답용)
            var h = new HotelDetailsDto.Hotel();
            h.setId(r.getHotelId());
            h.setName(r.getName());
            h.setRegion(r.getRegion());
            h.setAddress(r.getAddress());
            h.setGradeLevel(r.getGradeLevel());
            h.setCoverImageUrl(r.getCoverImageUrl());
            h.setRating(r.getRating() == null ? null : r.getRating().doubleValue()); // BigDecimal → Double 변환

            dto.setHotel(h);
            return dto;
        }).collect(Collectors.toList());

        // 페이지 응답 DTO 조립
        var res = new WishlistPageDto();
        res.setItems(items);                              // 현재 페이지 항목들
        res.setTotal(p.getTotalElements());               // 전체 개수
        res.setHasMore(p.hasNext());                      // 다음 페이지 존재 여부
        res.setNextOffset(p.hasNext() ? offset + p.getNumberOfElements() : offset); // 다음 오프셋 계산
        return res;
    }

    @DeleteMapping("/{id}") // DELETE /api/wishlists/{id}
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "username") String loginId, // ✅ 동일 방식으로 로그인 사용자 확인
            @PathVariable Long id // 삭제 대상 위시리스트 항목 ID
    ) {
        // 로그인 사용자의 ID 조회(미존재 시 401)
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        // 사용자의 항목이 맞는지 조회(없으면 404)
        var wl = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        repo.delete(wl); // 삭제 수행
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}
