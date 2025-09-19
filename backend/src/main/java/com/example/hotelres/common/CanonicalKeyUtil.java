package com.example.hotelres.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CanonicalKeyUtil {
    public static String forHotel(String name, String address) {
        try {
            String src = (name == null ? "" : name.trim().toLowerCase())
                       + "|" + (address == null ? "" : address.trim().toLowerCase());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
