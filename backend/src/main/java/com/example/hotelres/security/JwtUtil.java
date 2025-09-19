package com.example.hotelres.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private final Key key;
    private final long accessExpMs;
    private final long refreshExpMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    private String build(String subject, Map<String,Object> claims, long expMs){
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccess(String loginId, String role){
        return build(loginId, Map.of("type","access","role",role), accessExpMs);
    }
    public String generateRefresh(String loginId){
        return build(loginId, Map.of("type","refresh"), refreshExpMs);
    }

    public Jws<Claims> parse(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
    public boolean isAccessToken(String token){
        try { return "access".equals(parse(token).getBody().get("type")); }
        catch (Exception e){ return false; }
    }
    public long getRefreshExpMs(){ return refreshExpMs; }
}
