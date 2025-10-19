package com.example.hotelres.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*; // [NEW] for Set/Map/UUID

@Service
@RequiredArgsConstructor
public class ReviewFileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // [NEW] 서버 측에서도 허용 확장자 “화이트리스트”를 강제
    private static final Set<String> ALLOWED_EXT = Set.of("jpg","jpeg","png","webp","gif");

    // [NEW] 간단 매직바이트(시그니처) 확인표 — 실제 운영은 Apache Tika 권장
    private static final Map<String, byte[]> MAGIC_HEADERS = Map.of(
        "png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47},
        "jpg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF},
        "gif", new byte[]{0x47, 0x49, 0x46, 0x38} // "GIF8"
        // webp 매직은 "RIFF....WEBP"라 길어 간단검사는 생략(운영은 Tika 사용 권장)
    );

    // [NEW] (선택) 서버 측 최대 크기 — application.properties 제한 외에 이중 안전
    @Value("${app.upload.max-image-size-bytes:5242880}") // 5MB default
    private long maxImageSizeBytes;

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        // [NEW] (선택) 서버 측 크기 제한 이중 체크
        if (file.getSize() > maxImageSizeBytes) {
            throw new IllegalArgumentException("파일 용량이 허용치를 초과했습니다.");
        }

        String ext = extOf(file.getOriginalFilename());

        // [CHANGED] contentType만으로 신뢰하지 않고, 확장자 화이트리스트 우선
        if (!isAllowed(ext, file.getContentType(), file)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
        }

        String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext); // UUID 파일명 유지

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir  = root.resolve("reviews").resolve(ymd);

        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            // 정적 리소스 매핑: /files/** 로 접근 (기존 동작 유지)
            return "/files/reviews/" + ymd + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    private String extOf(String name) {
        if (!StringUtils.hasText(name)) return "";
        int i = name.lastIndexOf('.');
        return i > -1 ? name.substring(i + 1).toLowerCase() : "";
    }

    // [CHANGED] 허용 판단 로직 강화: 확장자 화이트리스트 + (가능 시) 매직바이트
    private boolean isAllowed(String ext, String contentType, MultipartFile file) {
        // 확장자 화이트리스트 우선
        if (!ALLOWED_EXT.contains(ext)) return false;

        // 간단 content-type 체크(신뢰 X, 보조)
        if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
            // 일부 클라이언트가 contentType을 비워 보내기도 하므로, 이 값만으로 거부하지는 않음
            // 여기서는 참고값으로만 사용
        }

        // 매직바이트 검사 (가능한 포맷에 대해)
        byte[] magic = MAGIC_HEADERS.get(ext);
        if (magic != null) {
            try (InputStream is = file.getInputStream()) {
                byte[] head = is.readNBytes(Math.max(4, magic.length));
                if (head.length < magic.length) return false;
                for (int i = 0; i < magic.length; i++) {
                    if (head[i] != magic[i]) return false;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteByUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) return false;
        try {
            // http(s)로 들어오면 path 부분만 추출
            if (publicUrl.startsWith("http")) {
                try {
                    publicUrl = java.net.URI.create(publicUrl).getPath();
                } catch (Exception ignore) { /* 무시 */ }
            }

            // /files/ 접두사를 파일시스템 경로로 치환
            String prefix = "/files/";
            String relative;
            if (publicUrl.startsWith(prefix)) {
                relative = publicUrl.substring(prefix.length()); // e.g. "reviews/20250925/uuid.jpg"
            } else {
                // 혹시 이미 상대경로 형태라면 그대로 사용
                relative = publicUrl.replaceFirst("^/+",""); // 보안상 선행 슬래시 제거
            }

            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();     // e.g. /.../uploads
            Path target = root.resolve(relative).normalize();                   // /.../uploads/reviews/...

            // 안전 가드(상위로 탈출 금지)
            if (!target.startsWith(root)) return false;

            return Files.deleteIfExists(target);
        } catch (Exception e) {
            return false; // 실패해도 호출부에서 무시할 수 있게 false만 반환
        }
    }
}
