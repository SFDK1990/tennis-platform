package com.tennisplatform.identity;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minimal cookie store, so a test can behave like a browser across several calls: keep what
 * the server sets, send it back on the next request. Needed because the refresh token only
 * ever travels as a cookie.
 */
public class CookieJar {

    private final Map<String, String> cookies = new LinkedHashMap<>();

    public void absorb(ResponseEntity<?> response) {
        var setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return;
        }
        for (String header : setCookies) {
            String pair = header.split(";", 2)[0];
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String name = pair.substring(0, separator);
            String value = pair.substring(separator + 1);
            if (value.isEmpty()) {
                cookies.remove(name);
            } else {
                cookies.put(name, value);
            }
        }
    }

    public String get(String name) {
        return cookies.get(name);
    }

    public boolean has(String name) {
        return cookies.containsKey(name);
    }

    public void put(String name, String value) {
        cookies.put(name, value);
    }

    public HttpHeaders asHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!cookies.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, cookies.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("; ")));
        }
        return headers;
    }
}
