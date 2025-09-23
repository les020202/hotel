package com.example.hotelres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication // 기본값: com.example.hotelres.* 전부 컴포넌트 스캔
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = {
        "com.example.hotelres" // ★ TossProps가 있는 패키지 트리 전체 스캔
// 필요하면 다른 @ConfigurationProperties 클래스가 있는 패키지도 추가
})
public class HotelresBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelresBackendApplication.class, args);
    }
}
