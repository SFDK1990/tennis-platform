package com.tennisplatform.identity.adapters.out.security;

import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
class SecureRandomTokenGenerator implements SecureTokenGenerator {

    /** 32 bytes = 256 bits. Enough that guessing is not a threat model worth modelling. */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generate() {
        byte[] buffer = new byte[TOKEN_BYTES];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer);
    }
}
