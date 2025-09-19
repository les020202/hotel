package com.example.hotelres.tosspayment;

import com.example.common.error.ApiException;
import com.example.hotelres.config.TossProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {
    private final TossProps props;
    private final RestTemplate rt = new RestTemplate();
    
    public void initCheck() {
        log.info(">>> Loaded Toss keys: clientKey={}, secretKey={}", 
                 props.getClientKey(), props.getSecretKey());
    }

    public Map<String, Object> confirm(String paymentKey, String orderId, int amount) {
    	String sk = props.getSecretKey();
    	if (sk == null || !(sk.startsWith("test_sk_") || sk.startsWith("live_sk_"))) {
    	    throw new ApiException(500, "CONFIG_ERROR",
    	        "시크릿키가 올바르지 않습니다. (예: test_sk_..., live_sk_...)");
    	}
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((props.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", basicAuth);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(
                Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount), headers);

        try {
            ResponseEntity<Map> res = rt.postForEntity(
                    "https://api.tosspayments.com/v1/payments/confirm", req, Map.class);
            Map<String, Object> body = res.getBody();
            log.info("Toss confirm 성공 orderId={} paymentKey={}", orderId, paymentKey);
            return body;
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.warn("Toss confirm 실패 status={} body={}", e.getRawStatusCode(), body);
            throw new ApiException(e.getRawStatusCode(), "TOSS_CONFIRM_FAILED",
                    "결제 승인에 실패했습니다. (" + e.getRawStatusCode() + ")");
        }
    }
}
