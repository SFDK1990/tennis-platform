package com.tennisplatform.teacher.configuration;

import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.UpdateTeacherProfile;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.application.service.GetTeacherProfileService;
import com.tennisplatform.teacher.application.service.UpdateTeacherProfileService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the use cases by hand, like {@code IdentityConfiguration} does, so the application layer
 * stays free of Spring stereotypes.
 */
@Configuration
@EnableConfigurationProperties(TeacherProperties.class)
public class TeacherConfiguration {

    @Bean
    public GetTeacherProfile getTeacherProfile(TeacherProfileRepository profiles) {
        return new GetTeacherProfileService(profiles);
    }

    @Bean
    public UpdateTeacherProfile updateTeacherProfile(TeacherProfileRepository profiles) {
        return new UpdateTeacherProfileService(profiles);
    }
}
