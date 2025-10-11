// src/main/java/com/example/hotelres/owner/OwnerSecurityUtil.java
package com.example.hotelres.owner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;

public final class OwnerSecurityUtil {
    private OwnerSecurityUtil(){}
    public static String resolveLoginId(Object principal) {
        if (principal instanceof UserDetails u) return u.getUsername();
        if (principal instanceof Map<?,?> m) {
            Object v = m.get("loginId"); if (v instanceof String s && !s.isBlank()) return s;
            Object sub = m.get("sub");   if (sub instanceof String s2 && !s2.isBlank()) return s2;
        }
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a!=null ? a.getName() : null);
    }
}
