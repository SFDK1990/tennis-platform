package com.tennisplatform.identity.adapters.out.email;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tennisplatform.identity.application.port.out.MailCooldown;
import com.tennisplatform.identity.configuration.IdentityProperties;
import com.tennisplatform.identity.domain.EmailAddress;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * In memory for the same reason as the rate limiter: there is a single instance. Bounded, so
 * an attacker cycling through addresses evicts the oldest entries instead of exhausting memory.
 * The times come from the application clock; the cache's own expiry only reclaims space.
 */
@Component
class InMemoryMailCooldown implements MailCooldown {

    private static final int MAX_TRACKED = 100_000;

    private final Duration cooldown;
    private final Cache<String, Instant> lastSent;

    InMemoryMailCooldown(IdentityProperties properties) {
        this.cooldown = properties.getMailCooldown();
        this.lastSent = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED)
                .expireAfterWrite(cooldown)
                .build();
    }

    @Override
    public boolean tryStart(Kind kind, EmailAddress recipient, Instant now) {
        String key = kind + ":" + recipient.value();
        boolean[] started = {false};
        lastSent.asMap().compute(key, (ignored, previous) -> {
            if (previous != null && now.isBefore(previous.plus(cooldown))) {
                return previous;
            }
            started[0] = true;
            return now;
        });
        return started[0];
    }
}
