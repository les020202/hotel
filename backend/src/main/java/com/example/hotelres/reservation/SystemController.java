package com.example.hotelres.reservation;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {
  @GetMapping("/api/time")
  public Map<String,String> now() {
    return Map.of("now", OffsetDateTime.now().toString());
  }
}

