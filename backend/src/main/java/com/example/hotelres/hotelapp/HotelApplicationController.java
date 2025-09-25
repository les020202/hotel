package com.example.hotelres.hotelapp;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.hotelres.security.CustomUserDetails;

@RestController
@RequestMapping("/api/hotelapp")
public class HotelApplicationController {

    private final HotelApplicationRepository repo;

    public HotelApplicationController(HotelApplicationRepository repo) {
        this.repo = repo;
    }

    /** 유저가 신청 저장 – 프론트가 보낸 바디를 그대로 엔티티로 받음 */
    @PostMapping
    @Transactional
    public ResponseEntity<HotelApplicationEntity> submit(
            @RequestBody HotelApplicationEntity body,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        body.setId(null);
        body.setUserId(user.getId()); // 로그인된 사용자 ID 강제 설정
        body.setStatus(HotelApplicationEntity.Status.PENDING);
        return ResponseEntity.ok(repo.save(body));
    }

    @GetMapping("/mine")
    public Page<HotelApplicationEntity> mine(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return repo.findByUserIdOrderByIdDesc(user.getId(), PageRequest.of(page, size));
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<HotelApplicationEntity> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
