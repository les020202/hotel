package com.example.hotelres.coupon;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * Coupon 엔티티
 * - 쿠폰(할인 코드)의 메타데이터를 보관합니다.
 * - JPA/Hibernate가 coupons 테이블과 매핑하고, Lombok이 보일러플레이트 코드를 생성합니다.
 *
 * 주요 포인트
 * 1) @Entity / @Table(name = "coupons")
 *    - 이 클래스가 JPA 엔티티이며 DB의 coupons 테이블과 매핑됨을 의미합니다.
 *
 * 2) Lombok 어노테이션
 *    - @Getter / @Setter : 모든 필드에 대해 getter/setter 자동 생성
 *    - @NoArgsConstructor : 파라미터 없는 기본 생성자 생성
 *    - @AllArgsConstructor : 모든 필드를 매개변수로 받는 생성자 생성
 *    - @Builder : 빌더 패턴으로 객체 생성 가능 (가독성↑, 선택적 필드 세팅↑)
 *
 * 3) 날짜/시간 타입
 *    - LocalDate : '날짜'만 (시/분/초 없음). 유효기간(validFrom, validTo)에 적합
 *    - LocalDateTime : '날짜+시간'. 생성 시각(createdAt) 기록에 적합
 *
 * 4) 유효기간 정책
 *    - validFrom / validTo 가 null이면 "제한 없음"으로 해석합니다.
 *      예) validFrom = null → 시작 제한 없음 / validTo = null → 만료 제한 없음
 *
 * 5) 금액/적용 정책
 *    - amount : 정액 할인 금액(KRW 기준)으로 해석 (예: 5000 → 5,000원 할인)
 *    - stackable : true면 다른 쿠폰과 함께 중복(스택) 적용 허용, false면 단독 적용만 허용
 *
 * 6) createdAt 기본값
 *    - 필드에 LocalDateTime.now()가 기본값으로 세팅되어 있어, 애플리케이션 레벨에서 생성 시각을 넣습니다.
 *    - columnDefinition의 "default current_timestamp"는 스키마 생성 시 DDL 힌트이며,
 *      JPA가 INSERT 시 createdAt을 명시적으로 넣으면 DB 기본값은 사용되지 않습니다.
 *      (둘 중 하나라도 채워지면 정상 동작. 단, DB/애플리케이션 타임존 차이 주의)
 *
 * 7) 제약사항/검증
 *    - code는 unique=true, length=50 → 중복 불가, 최대 50자
 *    - title은 length=100 → 최대 100자
 *    - amount / stackable / createdAt은 NOT NULL
 *    - 유효기간 로직(예: validFrom <= 오늘 <= validTo) 검증은 서비스/도메인 레벨에서 수행하는 것이 일반적
 */

@Entity @Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키. DB의 AUTO_INCREMENT(IDENTITY) 전략 사용

    @Column(nullable=false, length=50, unique=true)
    private String code; // 사용자에게 배포하는 "쿠폰 코드". 중복 금지

    @Column(nullable=false, length=100)
    private String title; // 쿠폰의 표시 이름/설명 제목 (예: "신규 가입 5천원 할인")

    @Column(nullable=false)
    private Integer amount; // 할인 금액(원화 기준 정액). 음수 금지(검증 필요)

    @Column(nullable=false)
    private Boolean stackable = false; // 다른 쿠폰과의 중복 적용 허용 여부 (기본: 불가)

    private LocalDate validFrom;  // 유효 시작일 (NULL = 시작 제한 없음)
    private LocalDate validTo;    // 유효 종료일 (NULL = 종료 제한 없음)

    @Column(nullable=false, columnDefinition = "datetime default current_timestamp")
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성 시각. 애플리케이션에서 즉시 값 세팅
}
