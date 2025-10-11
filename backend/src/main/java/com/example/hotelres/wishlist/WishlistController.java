// backend/src/main/java/com/example/hotelres/wishlist/WishlistController.java
package com.example.hotelres.wishlist;

import com.example.hotelres.admin.hotel.Hotel;
import com.example.hotelres.hotel.dto.HotelDetailsDto;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
import com.example.hotelres.wishlist.dto.WishlistItemDto;
import com.example.hotelres.wishlist.dto.WishlistPageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {

    private final WishlistRepository repo;
    private final UserRepository userRepository;
    private final EntityManager em;

    @Autowired
    public WishlistController(WishlistRepository repo,
                              UserRepository userRepository,
                              EntityManager em) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.em = em;
    }

    @GetMapping
    public WishlistPageDto list(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "4") int limit
    ) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        int page = offset / limit;
        var p = repo.findRowsByUserId(userId, PageRequest.of(page, limit));

        var items = p.getContent().stream().map(r -> {
            var dto = new WishlistItemDto();
            dto.setWishlistId(r.getWishlistId());
            dto.setCreatedAt(r.getCreatedAt());

            var h = new HotelDetailsDto.Hotel();
            h.setId(r.getHotelId());
            h.setName(r.getName());
            h.setRegion(r.getRegion());
            h.setAddress(r.getAddress());
            h.setGradeLevel(r.getGradeLevel());
            h.setCoverImageUrl(r.getCoverImageUrl());
            h.setRating(r.getRating() == null ? null : r.getRating().doubleValue());
            dto.setHotel(h);
            return dto;
        }).collect(Collectors.toList());

        var res = new WishlistPageDto();
        res.setItems(items);
        res.setTotal(p.getTotalElements());
        res.setHasMore(p.hasNext());
        res.setNextOffset(p.hasNext() ? offset + p.getNumberOfElements() : offset);
        return res;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @PathVariable Long id
    ) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        var wl = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        repo.delete(wl);
        return ResponseEntity.noContent().build();
    }

    // ✅ 변경: 저장/존재 확인 후, 프로젝션으로 다시 읽어서 DTO 작성(지연로딩 회피)
    @PostMapping
    public WishlistItemDto create(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @RequestBody CreateReq req
    ) {
        if (req == null || req.hotelId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hotelId is required");
        }
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        var existed = repo.findByUser_IdAndHotel_Id(userId, req.hotelId);
        Wishlist entity = existed.orElseGet(() -> {
            var wl = new Wishlist();
            wl.setUser(em.getReference(User.class, userId));
            wl.setHotel(em.getReference(Hotel.class, req.hotelId));
            return repo.save(wl);
        });

        var row = repo.findRowById(entity.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Wishlist created but not found"));

        var dto = new WishlistItemDto();
        dto.setWishlistId(row.getWishlistId());
        dto.setCreatedAt(row.getCreatedAt());

        var h = new HotelDetailsDto.Hotel();
        h.setId(row.getHotelId());
        h.setName(row.getName());
        h.setRegion(row.getRegion());
        h.setAddress(row.getAddress());
        h.setGradeLevel(row.getGradeLevel());
        h.setCoverImageUrl(row.getCoverImageUrl());
        h.setRating(row.getRating() == null ? null : row.getRating().doubleValue());
        dto.setHotel(h);

        return dto;
    }

    public static class CreateReq { public Long hotelId; }

    @GetMapping("/has")
    public HasRes has(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @RequestParam Long hotelId
    ) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        var found = repo.findByUser_IdAndHotel_Id(userId, hotelId);
        var res = new HasRes();
        res.wished = found.isPresent();
        res.wishlistId = found.map(w -> w.getId()).orElse(null);
        return res;
    }

    public static class HasRes { public boolean wished; public Long wishlistId; }

    @DeleteMapping("/by-hotel/{hotelId}")
    public ResponseEntity<Void> deleteByHotel(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @PathVariable Long hotelId
    ) {
        Long userId = userRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        if (!repo.existsByUser_IdAndHotel_Id(userId, hotelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        repo.deleteByUser_IdAndHotel_Id(userId, hotelId);
        return ResponseEntity.noContent().build();
    }
}
