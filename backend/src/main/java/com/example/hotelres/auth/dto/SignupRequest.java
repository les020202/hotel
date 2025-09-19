package com.example.hotelres.auth.dto;

import com.example.hotelres.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class SignupRequest {

    // 아이디: 8~20자, 영문/숫자만 (대소문자 허용하려면 A-Z 포함 유지)
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 8, max = 20, message = "아이디는 8~20자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{8,20}$",
             message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
    private String loginId;

    // 비밀번호: 10~30자, 소문자/숫자/특수 포함, 대문자 금지
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 10, max = 30, message = "비밀번호는 10~30자여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[~!@#$%^&*()_\\-+=\\[\\]{};:'\",.<>/?\\\\|`])(?!.*[A-Z]).{10,30}$",
        message = "비밀번호는 소문자/숫자/특수문자를 포함하고 대문자는 사용할 수 없습니다."
    )
    private String password;

    // 이름: 한글 10자(UTF-8 20바이트까지) → 화면에서 10자 제한, 서버는 20자 제한
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 20, message = "이름은 최대 20자까지 가능합니다.")
    private String name;

    // 이메일
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 최대 100자까지 가능합니다.")
    private String email;

    // 휴대폰: 010-1234-5678 형식 (프론트에서 포맷팅했다면 서버에서도 방어)
    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(
        regexp = "^010-\\d{4}-\\d{4}$",
        message = "휴대폰 번호는 010-xxxx-xxxx 형식이어야 합니다."
    )
    private String phone;

    // 성별: DB와 맞추기(MALE/FEMALE만). 기본값 주지 말고 필수로 받자.
    @NotNull(message = "성별은 필수입니다.")
    private User.Gender gender; // enum은 MALE, FEMALE만 있도록 정리

    // 생년월일: yyyy-MM-dd 로 받기 + 과거 날짜 + 성인 검증은 서비스에서 재확인
    @NotNull(message = "생년월일은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    // 컨트롤러가 @RequestParam/헤더로 받으므로 DTO에서 제거
    // private String verificationCode;
}