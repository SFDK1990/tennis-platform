package com.tennisplatform.platform.configuration;

import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import com.tennisplatform.platform.application.port.out.PlatformSettingsRepository;
import com.tennisplatform.platform.application.service.GetStudentLimitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the use cases by hand, keeping the application layer free of Spring stereotypes. */
@Configuration
public class PlatformConfiguration {

    @Bean
    public GetStudentLimit getStudentLimit(PlatformSettingsRepository settings) {
        return new GetStudentLimitService(settings);
    }
}
