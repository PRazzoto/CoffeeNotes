package com.example.coffeenotes.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtils {

    public static final long REFRESH_COOKIE_MAX_AGE_DAYS = 14;

    private CookieUtils() {}

    public static ResponseCookie buildRefreshCookie(String value, boolean secure) {
        return buildRefreshCookie(value, REFRESH_COOKIE_MAX_AGE_DAYS, secure);
    }

    public static ResponseCookie buildRefreshCookie(String value, long maxAgeDays, boolean secure) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(maxAgeDays))
                .build();
    }
}
