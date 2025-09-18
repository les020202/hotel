// src/main/java/com/example/hotelres/user/dto/UpdateEmailRequest.java
package com.example.hotelres.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmailRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String verificationCode; // 이메일 인증코드
}
