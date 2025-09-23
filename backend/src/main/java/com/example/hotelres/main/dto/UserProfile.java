package com.example.hotelres.main.dto;

import lombok.Data;

@Data
public class UserProfile {
    private Long id;
    private String loginId;
    private String nickname;
    private String[] roles;

    public UserProfile(Long id, String loginId, String nickname, String[] roles) {
        this.id = id; this.loginId = loginId; this.nickname = nickname; this.roles = roles;
    }
}
