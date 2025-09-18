package com.example.hotelres.api.dto; 
// DTO(Data Transfer Object)들을 모아둔 패키지

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
// 입력값 검증(Validation)용 어노테이션을 사용하기 위해 import

public class AccountDtos {
    // 마이페이지 관련 DTO들을 내부 record 형태로 정의한 클래스

    public record MeDto(
        Long id,                     // 회원 고유 ID
        String loginId,              // 로그인 아이디
        String email,                // 이메일 주소
        String name,                 // 이름
        String phone,                // 전화번호
        String address1,             // 기본 주소
        String address2,             // 상세 주소
        String postcode,             // 우편번호
        String gender,               // 성별(MALE/FEMALE/UNKNOWN)
        java.time.LocalDate birthDate, // 생년월일 (LocalDate)

        // ▼ 프로필/커버 이미지 관련 필드
        String profileImageType,     // 프로필 이미지 타입 (ex. jpg, png 등)
        String profileImageUrl,      // 프로필 이미지 실제 URL
        String profileImageTemplate, // 프로필 이미지 템플릿명

        String coverImageType,       // 커버 이미지 타입
        String coverImageUrl,        // 커버 이미지 실제 URL
        String coverImageTemplate    // 커버 이미지 템플릿명
    ) {}
    // ▶ MeDto : 마이페이지에서 내 정보 조회용 DTO

    // ▼ 이메일/생일은 변경 불가, 나머지 정보만 수정 가능
    public record MeUpdateDto(
        @NotBlank(message = "이름은 필수입니다.")
        String name, // 이름 (빈 값 불가)

        // 010-1234-5678 혹은 011/016~019-123-4567 형태만 허용
        @Pattern(
            regexp = "^(01[016-9])-\\d{3,4}-\\d{4}$",
            message = "휴대폰 번호는 010-1234-5678 형식이어야 합니다."
        )
        String phone, // 전화번호

        String address1, // 기본 주소
        String address2, // 상세 주소

        // 우편번호 5자리 또는 미입력 허용
        @Pattern(regexp = "^(\\d{5})?$", message = "우편번호는 5자리입니다.")
        String postcode, // 우편번호

        // MALE/FEMALE/UNKNOWN 중 하나(선택 입력)
        String gender
    ) {}
    // ▶ MeUpdateDto : 마이페이지에서 내 정보 수정 요청 DTO

    // 템플릿 선택 요청 DTO (프로필/커버 이미지 템플릿 변경 시 사용)
    public record TemplateReq(String template) {}
}
