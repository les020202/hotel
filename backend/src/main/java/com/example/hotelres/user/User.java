package com.example.hotelres.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "ux_users_login_id", columnList = "loginId", unique = true),
        @Index(name = "ux_users_email",    columnList = "email",   unique = true)
    }
)
@Getter
@Setter
public class User {

    public enum Gender { MALE, FEMALE, UNKNOWN }   // 값 목록은 유지 (null 허용)
    public enum Status { ACTIVE, LOCKED, INACTIVE }
    public enum Role { ROLE_USER, ROLE_ADMIN, ROLE_OWNER}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 191, unique = true)
    private String loginId;            // 아이디

    @Column(nullable = false)
    private String passwordHash;       // BCrypt

    @Column(nullable = false, length = 50)
    private String name;

    @Email
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    private String emailVerificationCode;

    // 이메일 인증 상태
    private boolean isEmailVerified = false;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address1;   // 도로명/지번

    @Column(length = 255)
    private String address2;   // 상세주소

    @Column(length = 10)
    private String postcode;   // 선택

    @Enumerated(EnumType.STRING)
    @Column(length = 10)       // ✅ nullable(기본값 true), 기본값 할당 제거 → null 허용
    private Gender gender;

    // ✅ 기본값 제거 → null 허용
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Role role = Role.ROLE_USER;

    // 로그인 실패 횟수
    private int failedLoginAttempts = 0;

    // 계정 잠금 시간
    private java.time.LocalDateTime lockedAt;
}
