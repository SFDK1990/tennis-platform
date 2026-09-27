package com.tennisplatform.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class UnexpectedErrorTest {

    /** An exception message can name tables, columns or values; none of it may reach the client. */
    @Test
    void aFailureNobodyAnticipatedTellsTheClientNothingAboutIt() {
        var failure = new IllegalStateException(
                "duplicate key value violates unique constraint \"users_email_key\" (email)=(ana@example.com)");

        ProblemDetail problem = new GlobalExceptionHandler().handleUnexpected(failure);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(problem.toString())
                .doesNotContain("users_email_key")
                .doesNotContain("ana@example.com")
                .doesNotContain("IllegalStateException");
    }

    /**
     * The log says what failed and where, and nothing of what the messages quoted: no personal
     * data reaches it, not even through a 500 (28-fase16-analisis-observabilidad.md).
     */
    @Test
    void theLogOfAnUnexpectedFailureKeepsTypesAndFramesButNoMessages(CapturedOutput output) {
        var cause = new IllegalArgumentException("Key (email)=(ana@example.com) already exists");
        var failure = new IllegalStateException("could not insert user ana@example.com", cause);

        new GlobalExceptionHandler().handleUnexpected(failure);

        assertThat(output.getAll())
                .contains("Unhandled exception")
                .contains("java.lang.IllegalStateException")
                .contains("Caused by: ")
                .contains("java.lang.IllegalArgumentException")
                .contains("at com.tennisplatform.error.UnexpectedErrorTest")
                .doesNotContain("ana@example.com")
                .doesNotContain("already exists");
    }
}
