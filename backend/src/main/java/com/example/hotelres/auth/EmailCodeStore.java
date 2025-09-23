package com.example.hotelres.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// com/example/hotelres/auth/EmailCodeStore.java
@Component
public class EmailCodeStore {
    private static class Entry { String code; Instant exp; }
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private String norm(String email){ return email == null ? null : email.trim().toLowerCase(); }

    public void save(String email, String code, int ttlSeconds) {
        var e = new Entry();
        e.code = code;
        e.exp = Instant.now().plusSeconds(ttlSeconds);
        store.put(norm(email), e);
    }

    /** 단순 확인(소모 X) */
    public boolean check(String email, String code) {
        var e = store.get(norm(email));
        if (e == null) return false;
        if (Instant.now().isAfter(e.exp)) { store.remove(norm(email)); return false; }
        return e.code.equals(code);
    }

    /** 최종 사용(성공 시 1회용 소모) */
    public boolean consume(String email, String code) {
        var key = norm(email);
        var e = store.get(key);
        if (e == null) return false;
        if (Instant.now().isAfter(e.exp)) { store.remove(key); return false; }
        boolean ok = e.code.equals(code);
        if (ok) store.remove(key);
        return ok;
    }

}
