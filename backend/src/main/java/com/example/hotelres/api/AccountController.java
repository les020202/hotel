// src/main/java/com/example/hotelres/api/AccountController.java
package com.example.hotelres.api;

import com.example.hotelres.api.dto.AccountDtos.MeDto;
import com.example.hotelres.api.dto.AccountDtos.MeUpdateDto;
import com.example.hotelres.api.dto.AccountDtos.TemplateReq;
import com.example.hotelres.common.CurrentUser;
import com.example.hotelres.user.User;
import com.example.hotelres.user.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AccountController {

  private final CurrentUser currentUser;
  private final UserRepository users;

  @Value("${app.upload-dir:uploads}")
  private String uploadDir;

  private static final long MAX_SIZE = 5L * 1024 * 1024; // 5MB
  private static final Set<String> ALLOWED_MIME = Set.of("image/png","image/jpeg","image/webp");

  /* ---------- (A) 텍스트 무해화(Sanitize) ---------- */
  private static String sanitize(String s) {
    if (s == null) return null;
    // <script>…</script> 제거
    s = s.replaceAll("(?is)<script.*?>.*?</script>", "");
    // 모든 태그 제거
    s = s.replaceAll("(?is)</?[^>]+>", "");
    // on* 핸들러 / javascript: 제거
    s = s.replaceAll("(?i)on[a-z]+\\s*=\\s*['\"][^'\"]*['\"]", "");
    s = s.replaceAll("(?i)javascript:", "");
    return s.trim();
  }

  /* ---------- (B) 확장자/시그니처 검증 유틸 ---------- */
  private static String safeExt(String filename, String contentType) {
    String ext = StringUtils.getFilenameExtension(filename);
    if (ext == null || ext.isBlank()) {
      if ("image/png".equals(contentType))  return "png";
      if ("image/jpeg".equals(contentType)) return "jpg";
      if ("image/webp".equals(contentType)) return "webp";
      return "dat";
    }
    return ext.toLowerCase(Locale.ROOT);
  }

  private static boolean looksLikePng(byte[] h) {
    return h.length >= 8 &&
        (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47 &&
        h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
  }
  private static boolean looksLikeJpeg(byte[] h) {
    return h.length >= 3 &&
        (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
  }
  private static boolean looksLikeWebp(byte[] h) {
    return h.length >= 12 &&
        h[0]=='R' && h[1]=='I' && h[2]=='F' && h[3]=='F' &&
        h[8]=='W' && h[9]=='E' && h[10]=='B' && h[11]=='P';
  }
  private static boolean looksLikeAllowed(byte[] head, String contentType) {
    return switch (contentType) {
      case "image/png"  -> looksLikePng(head);
      case "image/jpeg" -> looksLikeJpeg(head);
      case "image/webp" -> looksLikeWebp(head);
      default -> false;
    };
  }

  private String makeSafeName(String prefix, String ext) {
    String ts = String.valueOf(System.currentTimeMillis());
    return "%s-%s-%s.%s".formatted(prefix, ts, UUID.randomUUID(), ext);
  }

  private String saveFile(MultipartFile file, String prefix) throws IOException {
    if (file == null || file.isEmpty())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다.");
    if (file.getSize() > MAX_SIZE)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기(5MB) 초과");

    String ct = file.getContentType();
    if (ct == null || !ALLOWED_MIME.contains(ct))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PNG/JPEG/WebP만 허용");

    // 매직바이트 검사
    byte[] head;
    try (var in = file.getInputStream()) { head = in.readNBytes(16); }
    if (!looksLikeAllowed(head, ct))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 시그니처가 유효하지 않음");

    Files.createDirectories(Paths.get(uploadDir));
    String ext = safeExt(file.getOriginalFilename(), ct);
    if (!Set.of("png","jpg","jpeg","webp").contains(ext)) {
      if ("image/png".equals(ct)) ext = "png";
      else if ("image/jpeg".equals(ct)) ext = "jpg";
      else if ("image/webp".equals(ct)) ext = "webp";
    }
    String name = makeSafeName(prefix, ext);
    Path target = Paths.get(uploadDir).resolve(name).normalize();
    try (var in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return "/files/" + name;
  }

  /* ---------- (C) 내 정보 조회 ---------- */
  @GetMapping("/me")
  public MeDto me(Authentication auth) {
    User u = currentUser.get(auth);
    return new MeDto(
        u.getId(), u.getLoginId(), u.getEmail(), u.getName(), u.getPhone(),
        u.getAddress1(), u.getAddress2(), u.getPostcode(),
        u.getGender()!=null ? u.getGender().name() : null, u.getBirthDate(),
        u.getProfileImageType()!=null ? u.getProfileImageType().name() : "NONE",
        u.getProfileImageUrl(),
        u.getProfileImageTemplate()!=null ? u.getProfileImageTemplate().name() : null,
        u.getCoverImageType()!=null ? u.getCoverImageType().name() : "NONE",
        u.getCoverImageUrl(),
        u.getCoverImageTemplate()!=null ? u.getCoverImageTemplate().name() : null
    );
  }

  /* ---------- (D) 기본 정보 수정: @Valid + sanitize ---------- */
  @PutMapping("/me")
  public void update(Authentication auth, @Valid @RequestBody MeUpdateDto dto) {
    User u = currentUser.get(auth);
    if (StringUtils.hasText(dto.name()))  u.setName(sanitize(dto.name()));
    if (StringUtils.hasText(dto.phone())) u.setPhone(sanitize(dto.phone()));
    u.setAddress1(sanitize(dto.address1()));
    u.setAddress2(sanitize(dto.address2()));
    u.setPostcode(sanitize(dto.postcode()));
    if (StringUtils.hasText(dto.gender())) {
      try { u.setGender(User.Gender.valueOf(dto.gender())); } catch (IllegalArgumentException ignore) {}
    }
    users.save(u);
  }

  /* ---------- (E) 프로필/커버 업로드 & 템플릿 ---------- */
  @PostMapping(path="/me/profile/upload", consumes="multipart/form-data")
  public MeDto uploadProfile(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
    User u = currentUser.get(auth);
    String url = saveFile(file, "profile-" + u.getId());
    u.setProfileImageType(User.ImageType.UPLOADED);
    u.setProfileImageUrl(url);
    u.setProfileImageTemplate(null);
    users.save(u);
    return me(auth);
  }

  @PutMapping("/me/profile/template")
  public MeDto setProfileTemplate(Authentication auth, @Valid @RequestBody TemplateReq req) {
    User u = currentUser.get(auth);
    try {
      User.ProfileTpl tpl = User.ProfileTpl.valueOf(req.template().toUpperCase());
      u.setProfileImageType(User.ImageType.TEMPLATE);
      u.setProfileImageTemplate(tpl);
      u.setProfileImageUrl(null);
      users.save(u);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 프로필 템플릿 코드");
    }
    return me(auth);
  }

  @PostMapping(path="/me/cover/upload", consumes="multipart/form-data")
  public MeDto uploadCover(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
    User u = currentUser.get(auth);
    String url = saveFile(file, "cover-" + u.getId());
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
}
