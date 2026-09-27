package com.tennisplatform.platform.configuration;

import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import com.tennisplatform.platform.application.port.in.ManagePlatformSettings;
import com.tennisplatform.platform.application.service.PlatformLimitsService;
import com.tennisplatform.platform.application.service.PlatformSettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Wires the use cases by hand, keeping the application layer free of Spring stereotypes. */
@Configuration
public class PlatformConfiguration {

    /**
     * One bean behind both ports, declared by its concrete type so that injection by either
     * interface finds exactly one candidate. Declaring a bean per port as well would register
     * the same object three times and make every injection ambiguous.
     */
    @Bean
    public PlatformLimitsService platformLimits(PlatformSettingsRepository settings) {
        return new PlatformLimitsService(settings);
    }

    @Bean
    public ManagePlatformSettings managePlatformSettings(PlatformSettingsRepository settings, Clock clock) {
        return new PlatformSettingsService(settings, clock);
    }
}
