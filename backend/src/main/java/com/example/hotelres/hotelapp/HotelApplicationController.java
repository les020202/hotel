package com.example.hotelres.hotelapp;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<HotelApplicationEntity> submit(@RequestBody HotelApplicationEntity body) {
        body.setId(null);
        body.setStatus(HotelApplicationEntity.Status.PENDING);
        return ResponseEntity.ok(repo.save(body));
    }

    /** 내 신청 목록 (로그인 연동 시 userId 세팅해서 사용) */
    @GetMapping("/mine")
    public Page<HotelApplicationEntity> mine(@RequestParam Long userId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return repo.findByUserIdOrderByIdDesc(userId, PageRequest.of(page, size));
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<HotelApplicationEntity> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
