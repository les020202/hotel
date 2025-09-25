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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewFileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String ext = extOf(file.getOriginalFilename());
        if (!isAllowed(ext, file.getContentType())) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
        }

        String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir  = root.resolve("reviews").resolve(ymd);

        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            // 정적 리소스 매핑: /files/** 로도 접근 가능
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

    private boolean isAllowed(String ext, String contentType) {
        if (contentType != null && contentType.startsWith("image/")) return true;
        return switch (ext) {
            case "jpg", "jpeg", "png", "webp", "gif" -> true;
            default -> false;
        };
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
                relative = publicUrl.replaceFirst("^/+","");
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
