package com.tennisplatform.error;

import com.tennisplatform.AbstractIntegrationTest;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole stack, with a filter that fails the way a real one could: the answer is the
 * contract's 500, the request log counts it, and the message it carried reaches nobody.
 */
@ExtendWith(OutputCaptureExtension.class)
@Import(FailureInAFilterTest.FailingFilter.class)
class FailureInAFilterTest extends AbstractIntegrationTest {

    private static final String PERSONAL_DATA = "lucia@example.com";

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class FailingFilter {

        /** Ahead of the security chain, where no controller advice ever runs. */
        @Bean
        FilterRegistrationBean<Filter> failingFilter() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
                throw new IllegalStateException("no row for " + PERSONAL_DATA);
            });
            registration.addUrlPatterns("/api/v1/failing");
            registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 20);
            return registration;
        }
    }

    @Test
    void aFailureInAFilterAnswersTheSame500AndLeavesNoMessageInTheLog(CapturedOutput output) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/failing")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("INTERNAL_ERROR").doesNotContain(PERSONAL_DATA);
        assertThat(output.getAll())
                .contains("GET /api/v1/failing 500 INTERNAL_ERROR")
                .contains("java.lang.IllegalStateException")
                .doesNotContain(PERSONAL_DATA);
    }
}
