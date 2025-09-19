package com.example.hotelres.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {
    private final UserRepository users;

    @GetMapping("/me") // JWT 없으면 Security에서 401
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }

        var opt = users.findByLoginId(principal.getUsername());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "USER_NOT_FOUND"));
        }

        var u = opt.get();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("id", u.getId());
        body.put("loginId", u.getLoginId());
        body.put("name", u.getName());                                   // null 허용
        body.put("email", u.getEmail());                                 // null 허용
        body.put("phone", u.getPhone());                                 // null 허용
        body.put("status", u.getStatus() != null ? u.getStatus().name() : null);
        body.put("role",   u.getRole()   != null ? u.getRole().name()   : null);

        return ResponseEntity.ok(body);
    }
}


