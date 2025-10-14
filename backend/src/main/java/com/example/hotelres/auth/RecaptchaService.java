package com.example.hotelres.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class RecaptchaService {
    @Value("${recaptcha.secret}")
    private String secret;

    private final RestTemplate rest = new RestTemplate();
    private final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verify(String token) {
        if (token == null || token.isBlank()) return false;
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secret);
        body.add("response", token);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rest.postForObject(VERIFY_URL, body, Map.class);
            if (resp == null) return false;
            Object success = resp.get("success");
            if (success instanceof Boolean) {
                return (Boolean) success;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
