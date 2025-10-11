// src/main/java/com/example/hotelres/config/StaticResourceConfig.java
package com.example.hotelres.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    // 실제 저장 폴더 (둘 다 여기로 매핑)
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    public void init() throws Exception {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        // 필요시 하위 디렉토리 미리 생성
        Files.createDirectories(root.resolve("reviews"));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // OS 독립적인 file: URL 로 변환 (예: file:/C:/... 또는 file:/var/...)
        String location = root.toUri().toString();
        // 기존 사용하던 /files/** 유지
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        // 새로 추가하는 /uploads/** 도 동일 폴더로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
