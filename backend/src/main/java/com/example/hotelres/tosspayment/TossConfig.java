// path: src/main/java/com/example/hotelres/tosspayment/TossConfig.java
package com.example.hotelres.tosspayment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TossProps.class)
public class TossConfig { }