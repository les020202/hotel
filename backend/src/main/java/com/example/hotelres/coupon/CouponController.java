package com.example.hotelres.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
 * CouponController
 * - 쿠폰 API를 처리하는 REST 컨트롤러입니다.
 * - 클래스 레벨 경로(prefix): /api/coupons
 */
@RestController // 반환값을 HTTP 응답 바디(JSON)로 직렬화하는 컨트롤러
@RequestMapping("/api/coupons") // 이 컨트롤러의 모든 메서드 앞에 붙는 기본 경로
@RequiredArgsConstructor // final 필드를 파라미터로 하는 생성자를 자동 생성(생성자 주입)
public class CouponController {

    private final CouponRepository coupons; // 쿠폰 데이터를 조회/처리하는 리포지토리 의존성

    // GET /api/coupons          -> 오늘 사용 가능한 것만 조회
    // GET /api/coupons?all=true -> 전체(만료 포함) 조회
    @GetMapping // HTTP GET 요청을 처리하는 메서드
    public List<Coupon> list(@RequestParam(name="all", defaultValue = "false") boolean all) {
        // 요청 파라미터 all을 boolean으로 받음 (기본값 false)
        // all=false → !all == true  → 오늘 사용 가능한 것만 조회
        // all=true  → !all == false → 만료 포함 전체 조회
        return coupons.findForList(!all ? true : false);
    }
}
