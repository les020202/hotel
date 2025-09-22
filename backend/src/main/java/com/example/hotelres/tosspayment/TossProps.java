// path: src/main/java/com/example/hotelres/tosspayment/TossProps.java
package com.example.hotelres.tosspayment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payments.toss")
public record TossProps(String baseUrl, String secretKey) {}
