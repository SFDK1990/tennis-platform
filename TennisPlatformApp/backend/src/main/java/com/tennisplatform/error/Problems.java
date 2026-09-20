package com.tennisplatform.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds the error body every module answers with.
 *
 * <p>Each module maps its own domain exceptions - the boundary rules keep {@code error} from
 * knowing what an {@code availability} exception is, and that is the right shape. What is not
 * module-specific is the body: status, title, detail and the {@code code} the frontend switches
 * on. That had been copied into all four module advices character for character, which is how a
 * fifth module ends up naming the property {@code errorCode} and nobody notices until a client
 * stops recognising an error.
 *
 * <p>{@link ProblemDetailWriter} writes the same shape from inside the security filter chain,
 * where no advice ever runs.
 */
public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
