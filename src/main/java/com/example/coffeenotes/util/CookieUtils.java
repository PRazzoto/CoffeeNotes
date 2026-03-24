package com.example.coffeenotes.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {

    public static final long REFRESH_COOKIE_MAX_AGE_DAYS = 14;
    public static final String DEFAULT_SAME_SITE = "Lax";

    private CookieUtils() {}

    public static ResponseCookie buildRefreshCookie(String value, boolean secure, String sameSite) {
        return buildRefreshCookie(value, REFRESH_COOKIE_MAX_AGE_DAYS, secure, sameSite);
    }

    public static ResponseCookie buildRefreshCookie(String value, long maxAgeDays, boolean secure, String sameSite) {
        String resolvedSameSite = sameSite == null || sameSite.isBlank() ? DEFAULT_SAME_SITE : sameSite.trim();
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite(resolvedSameSite)
                .maxAge(Duration.ofDays(maxAgeDays))
                .build();
    }
}
