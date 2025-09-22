
package com.example.hotelres.user;  
// 사용자(User) 관련 패키지


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "ux_users_login_id", columnList = "loginId", unique = true),
        @Index(name = "ux_users_email",    columnList = "email",   unique = true)
    }
)

// JPA 엔티티 매핑, 테이블명: users
// loginId, email 은 유니크 인덱스 설정
@Getter
@Setter
// Lombok: getter/setter 자동 생성
public class User {

    // === Enum 정의 ===
    public enum Gender { MALE, FEMALE, UNKNOWN }            // 성별
    public enum Status { ACTIVE, LOCKED, INACTIVE, DELETED } // 계정 상태
    public enum Role { ROLE_USER, ROLE_ADMIN, ROLE_OWNER }   // 권한
    public enum ImageType { NONE, UPLOADED, TEMPLATE }       // 이미지 타입

    // 프로필 기본 이미지 템플릿
    public enum ProfileTpl { T1, T2, T3 }

    // 커버 기본 이미지 템플릿
    public enum CoverTpl { C1, C2, C3 }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PK, AUTO_INCREMENT

    @Column(nullable = false, length = 191, unique = true)
    private String loginId; // 아이디 (로그인용, 유니크)

    @Column(nullable = false)
    private String passwordHash; // 비밀번호 해시 (BCrypt)

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Email
    @Column(nullable = false, length = 100, unique = true)
    private String email; // 이메일 (유니크)

    private String emailVerificationCode; // 이메일 인증 코드
    private boolean isEmailVerified = false; // 이메일 인증 여부

    @Column(length = 20)
    private String phone; // 전화번호

    @Column(length = 255)
    private String address1; // 도로명/지번 주소

    @Column(length = 255)
    private String address2; // 상세 주소

    @Column(length = 10)
    private String postcode; // 우편번호

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender; // 성별 (MALE/FEMALE/UNKNOWN)

    private LocalDate birthDate; // 생년월일

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status = Status.ACTIVE; // 계정 상태 (기본: ACTIVE)

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Role role = Role.ROLE_USER; // 권한 (기본: 일반 사용자)

    // 로그인 실패 관련
    private int failedLoginAttempts = 0; // 로그인 실패 횟수
    private LocalDateTime lockedAt;      // 계정 잠김 시간

    // === 프로필 이미지 ===
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ImageType profileImageType = ImageType.NONE; // 업로드/템플릿/없음

    @Column(length = 300)
    private String profileImageUrl; // 업로드된 프로필 이미지 경로

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private ProfileTpl profileImageTemplate; // 기본 템플릿 코드 (T1/T2/T3)

    // === 커버 이미지 ===
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ImageType coverImageType = ImageType.NONE; // 업로드/템플릿/없음

    @Column(length = 300)
    private String coverImageUrl; // 업로드된 커버 이미지 경로

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private CoverTpl coverImageTemplate; // 기본 템플릿 코드 (C1/C2/C3)

    @CreationTimestamp
    private LocalDateTime createdAt; // 생성일 자동 기록

    @UpdateTimestamp
    private LocalDateTime updatedAt; // 수정일 자동 기록

}
