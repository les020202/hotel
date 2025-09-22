package com.example.hotelres.config;  
// 설정 관련 패키지

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
// 스프링 설정 클래스임을 표시
public class StaticResourceConfig implements WebMvcConfigurer {

    // application.properties 에서 업로드 경로(app.upload-dir) 읽음. 없으면 기본값 "uploads"
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private Path root; // 실제 업로드 디렉토리 경로 객체

    @PostConstruct
    public void init() throws Exception {
        root = Paths.get(uploadDir).toAbsolutePath().normalize(); // 절대 경로로 변환
        Files.createDirectories(root); // 업로드 디렉토리가 없으면 생성
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들러 등록
        // URL: http://localhost:8888/files/**  → 실제 폴더: {uploadDir}/**
        // root.toUri().toString() → "file:/..." 형태라 운영체제 상관없이 안전

        registry.addResourceHandler("/files/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod(3600); // 캐시 유지 시간(초). 필요 없으면 제거 가능
    }
}
