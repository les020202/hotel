package com.example.hotelres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.example.hotelres.config")
public class HotelresBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelresBackendApplication.class, args);
    }
}
