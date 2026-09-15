package com.tennisplatform.identity.adapters.out.security;

import com.tennisplatform.identity.application.port.out.TokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
class Sha256TokenHasher implements TokenHasher {

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java platform; this cannot happen at runtime.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
