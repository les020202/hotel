// path: src/main/java/com/example/hotelres/tosspayment/TossPaymentService.java
package com.example.hotelres.tosspayment;

import com.example.hotelres.common.ApiException;
import com.example.hotelres.tosspayment.dto.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final TossProps props; // record TossProps(String baseUrl, String secretKey)

    private RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return new RestTemplate(factory);
    }

    public TossConfirmResponse confirm(String paymentKey, String orderId, long amount) {
        log.info("Toss Confirm 요청: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        // ✅ record 접근자 사용
        if (props.secretKey() == null || props.secretKey().isBlank())
            throw new ApiException("Toss secret-key 미설정");
        if (props.baseUrl() == null || props.baseUrl().isBlank())
            throw new ApiException("Toss base-url 미설정");

        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((props.secretKey() + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", basicAuth);

        Map<String,Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );
        HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, headers);
        String url = props.baseUrl() + "/v1/payments/confirm";

        try {
            ResponseEntity<TossConfirmResponse> res = restTemplate().exchange(
                    url, HttpMethod.POST, req, TossConfirmResponse.class);

            TossConfirmResponse dto = res.getBody();
            if (dto == null) throw new ApiException("Toss 응답 바디가 비어있음");

            if (!"DONE".equalsIgnoreCase(dto.status()))
                throw new ApiException("승인 상태 아님: " + dto.status());
            if (dto.totalAmount() == null || dto.totalAmount() != amount)
                throw new ApiException("금액 불일치(totalAmount=" + dto.totalAmount() + ", req=" + amount + ")");
            if (!orderId.equals(dto.orderId()))
                throw new ApiException("주문번호 불일치(resp=" + dto.orderId() + ", req=" + orderId + ")");

            log.info("Toss Confirm 성공: paymentKey={}, amount={}", dto.paymentKey(), dto.totalAmount());
            return dto;

        } catch (HttpStatusCodeException e) {
            String response = e.getResponseBodyAsString();
            log.warn("Toss Confirm 실패: status={}, body={}", e.getStatusCode(), response);
            throw new ApiException("Toss confirm 실패(" + e.getStatusCode() + "): " + response, e);
        } catch (Exception e) {
            throw new ApiException("Toss confirm 예외: " + e.getMessage(), e);
        }
    }
}
