package com.example.hotelres.api;  
// API 계층의 컨트롤러 클래스들이 모여 있는 패키지

import com.example.hotelres.api.dto.AccountDtos.MeDto;
import com.example.hotelres.api.dto.AccountDtos.MeUpdateDto;
import com.example.hotelres.api.dto.AccountDtos.TemplateReq;
// 마이페이지 관련 DTO (조회/수정/템플릿 요청 DTO) import

import com.example.hotelres.common.CurrentUser;
// 현재 인증된 사용자 정보를 가져오는 헬퍼 컴포넌트

import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;
// User 엔티티와 해당 JPA Repository

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
// 입력값 검증 및 Lombok 의존성 주입

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
// 스프링 웹 관련 클래스들

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
// 파일 저장, 경로, 컬렉션, 랜덤 UUID 생성 등 유틸

@RestController
@RequestMapping("/api/users")
// REST 컨트롤러, "/api/users" 경로로 매핑되는 클래스
@RequiredArgsConstructor
// final 필드에 대해 생성자 자동 주입
public class AccountController {

  private final CurrentUser currentUser; // 현재 로그인한 사용자 가져오는 헬퍼
  private final UserRepository users;    // User 엔티티 저장/조회 JPA Repo

  @Value("${app.upload-dir:uploads}")
  private String uploadDir; // 업로드 파일 저장 경로 (환경설정 없으면 "uploads" 기본값)

  private static final Set<String> ALLOWED_MIME = Set.of(
      "image/png", "image/jpeg", "image/webp"
  );
  // 허용할 이미지 MIME 타입 (png, jpeg, webp만 허용)

  // 파일 확장자 안전하게 추출하는 메서드
  private static String safeExt(String filename, String contentType) {
    String ext = StringUtils.getFilenameExtension(filename);
    if (ext == null || ext.isBlank()) {
      // 확장자가 없으면 contentType 으로 유추
      if ("image/png".equals(contentType))  return "png";
      if ("image/jpeg".equals(contentType)) return "jpg";
      if ("image/webp".equals(contentType)) return "webp";
      return "dat"; // 알 수 없을 경우 기본 확장자
    }
    return ext.toLowerCase(Locale.ROOT); // 확장자 소문자 처리
  }

  // 업로드된 파일을 서버 디렉토리에 저장하는 메서드
  private String saveFile(MultipartFile file, String prefix) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.");
    }
    if (file.getContentType() == null || !ALLOWED_MIME.contains(file.getContentType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다.");
    }

    Files.createDirectories(Paths.get(uploadDir)); // 저장 폴더 생성 (없으면)

    String ext = safeExt(file.getOriginalFilename(), file.getContentType()); // 확장자 추출
    String name = "%s-%s.%s".formatted(prefix, UUID.randomUUID(), ext); // 고유 파일명 생성

    Path target = Paths.get(uploadDir, name);
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); 
    // 파일 저장

    return "/files/" + name; // 저장된 파일 접근 경로 리턴
  }

  // ===== 조회 =====
  @GetMapping("/me")
  public MeDto me(Authentication auth) {
    User u = currentUser.get(auth); // 현재 로그인한 User 엔티티 가져오기
    return new MeDto(
        u.getId(),
        u.getLoginId(),
        u.getEmail(),
        u.getName(),
        u.getPhone(),
        u.getAddress1(),
        u.getAddress2(),
        u.getPostcode(),
        u.getGender() != null ? u.getGender().name() : null,
        u.getBirthDate(),
        // profile
        u.getProfileImageType() != null ? u.getProfileImageType().name() : "NONE",
        u.getProfileImageUrl(),
        u.getProfileImageTemplate() != null ? u.getProfileImageTemplate().name() : null,
        // cover
        u.getCoverImageType() != null ? u.getCoverImageType().name() : "NONE",
        u.getCoverImageUrl(),
        u.getCoverImageTemplate() != null ? u.getCoverImageTemplate().name() : null
    );
  }
  // ▶ 내 정보(Me) 조회 API

  // ===== 기본 정보 수정 =====
  @PutMapping("/me")
  public void update(Authentication auth, @RequestBody MeUpdateDto dto) {
    User u = currentUser.get(auth); // 현재 로그인 User

    if (StringUtils.hasText(dto.name()))     u.setName(dto.name());
    if (StringUtils.hasText(dto.phone()))    u.setPhone(dto.phone());
    u.setAddress1(dto.address1());
    u.setAddress2(dto.address2());
    u.setPostcode(dto.postcode());

    if (StringUtils.hasText(dto.gender())) {
      try {
        u.setGender(User.Gender.valueOf(dto.gender()));
      } catch (IllegalArgumentException ignore) {}
      // 잘못된 gender 값이 들어와도 무시
    }

    users.save(u); // 변경 내용 저장
  }
  // ▶ 기본 정보(이름, 연락처, 주소, 성별) 수정 API

  // ===== 프로필 업로드 / 템플릿 선택 =====
  @PostMapping(path = "/me/profile/upload", consumes = "multipart/form-data")
  public MeDto uploadProfile(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
    User u = currentUser.get(auth);
    String url = saveFile(file, "profile-" + u.getId()); // 파일 저장
    u.setProfileImageType(User.ImageType.UPLOADED); // 업로드 타입으로 지정
    u.setProfileImageUrl(url);                      // 저장된 URL 기록
    u.setProfileImageTemplate(null);                // 템플릿 값 초기화
    users.save(u);
    return me(auth); // 수정 후 최신 정보 반환
  }

  @PutMapping("/me/profile/template")
  public MeDto setProfileTemplate(Authentication auth, @Valid @RequestBody TemplateReq req) {
    User u = currentUser.get(auth);
    try {
      User.ProfileTpl tpl = User.ProfileTpl.valueOf(req.template().toUpperCase());
      u.setProfileImageType(User.ImageType.TEMPLATE);
      u.setProfileImageTemplate(tpl);
      u.setProfileImageUrl(null); // URL은 null로 초기화
      users.save(u);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 프로필 템플릿 코드");
    }
    return me(auth);
  }
  // ▶ 프로필 이미지 업로드/템플릿 선택 API

  // ===== 커버 업로드 / 템플릿 선택 =====
  @PostMapping(path = "/me/cover/upload", consumes = "multipart/form-data")
  public MeDto uploadCover(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
    User u = currentUser.get(auth);
    String url = saveFile(file, "cover-" + u.getId()); // 파일 저장
    u.setCoverImageType(User.ImageType.UPLOADED);
    u.setCoverImageUrl(url);
    u.setCoverImageTemplate(null);
    users.save(u);
    return me(auth);
  }

  @PutMapping("/me/cover/template")
  public MeDto setCoverTemplate(Authentication auth, @Valid @RequestBody TemplateReq req) {
    User u = currentUser.get(auth);
    try {
      User.CoverTpl tpl = User.CoverTpl.valueOf(req.template().toUpperCase());
      u.setCoverImageType(User.ImageType.TEMPLATE);
      u.setCoverImageTemplate(tpl);
      u.setCoverImageUrl(null);
      users.save(u);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 커버 템플릿 코드");
    }
    return me(auth);
  }
  // ▶ 커버 이미지 업로드/템플릿 선택 API
}
