package com.tennisplatform.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A failure in a filter never reaches {@link GlobalExceptionHandler}. It must still answer the
 * contract's 500 and leave no message in the log: Tomcat, which would log it otherwise, writes
 * the message whole.
 */
@ExtendWith(OutputCaptureExtension.class)
class UnhandledFailureFilterTest {

    private final UnhandledFailureFilter filter = new UnhandledFailureFilter(new ProblemDetailWriter(new ObjectMapper()));

    @Test
    void aFailureOutsideTheControllersAnswersTheSame500AndLogsNoMessage(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/lessons/1/bookings");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new IllegalStateException("no row for lucia@example.com");
        });

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("INTERNAL_ERROR").doesNotContain("lucia@example.com");
        assertThat(ProblemCode.of(request)).isEqualTo("INTERNAL_ERROR");
        assertThat(output.getAll())
                .contains("Unhandled exception")
                .contains("java.lang.IllegalStateException")
                .doesNotContain("lucia@example.com");
    }
}
