package com.tennisplatform.error;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The business code of the error a request answered with, kept on the request so the request log
 * line and the {@code http.server.requests} metric can say which conflict it was, not only that
 * it was a 409 (28-fase16-analisis-observabilidad.md).
 *
 * <p>Three places write an error, and each records its code here: {@link ProblemCodeAdvice} for
 * every controller advice, {@link ProblemDetailWriter} for the security chain, and the auth rate
 * limiter, which answers before either.
 */
public final class ProblemCode {

    /** What a request that did not fail is tagged with. */
    public static final String NONE = "none";

    private static final String ATTRIBUTE = ProblemCode.class.getName();

    private ProblemCode() {
    }

    public static void record(HttpServletRequest request, String code) {
        request.setAttribute(ATTRIBUTE, code);
    }

    public static String of(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof String code ? code : NONE;
    }
}
