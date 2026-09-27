package com.tennisplatform.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

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
}
