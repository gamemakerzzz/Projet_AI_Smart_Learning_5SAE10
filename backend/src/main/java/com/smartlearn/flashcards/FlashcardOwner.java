package com.smartlearn.flashcards;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

@Component
public class FlashcardOwner {
    public static final String COOKIE = "SMARTLEARN_FLASHCARDS";
    private static final String SESSION = "flashcard-browser-key";
    private final SecureRandom random = new SecureRandom();
    public String key(HttpServletRequest request, HttpServletResponse response) {
        String value = null;
        if (request.getCookies() != null) for (var cookie : request.getCookies())
            if (COOKIE.equals(cookie.getName()) && cookie.getValue().matches("[a-f0-9]{64}")) value = cookie.getValue();
        if (value == null) {
            Object previous = request.getSession().getAttribute(SESSION);
            if (previous instanceof String existing) value = existing;
            else { byte[] bytes = new byte[32]; random.nextBytes(bytes); value = HexFormat.of().formatHex(bytes); }
            var cookie = new Cookie(COOKIE, value); cookie.setHttpOnly(true); cookie.setSecure(request.isSecure());
            cookie.setPath("/api/flashcards"); cookie.setMaxAge(365 * 24 * 60 * 60); cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
        }
        request.getSession().setAttribute(SESSION, value);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
