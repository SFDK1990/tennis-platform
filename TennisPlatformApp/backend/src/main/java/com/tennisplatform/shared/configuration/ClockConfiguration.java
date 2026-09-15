package com.tennisplatform.shared.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Time is injected, never read statically. Every expiry rule in the platform - token
 * lifetimes, the 24 hour cancellation window - then becomes testable without waiting.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
