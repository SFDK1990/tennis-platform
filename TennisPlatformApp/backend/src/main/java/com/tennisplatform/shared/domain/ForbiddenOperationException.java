package com.tennisplatform.shared.domain;

/**
 * The caller is authenticated but may not do this. Carries its own Problem Details code, so one
 * class serves every module instead of a copy per module.
 */
public class ForbiddenOperationException extends RuntimeException {

    public static final String TEACHER_FORBIDDEN = "TEACHER_FORBIDDEN";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";

    private final String code;

    public ForbiddenOperationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Only the teacher who owns what is being touched may do this. */
    public static ForbiddenOperationException teacherOnly(String message) {
        return new ForbiddenOperationException(TEACHER_FORBIDDEN, message);
    }

    /** The caller's role is not one this operation serves. */
    public static ForbiddenOperationException roleNotAllowed(String message) {
        return new ForbiddenOperationException(AUTH_FORBIDDEN, message);
    }

    public String code() {
        return code;
    }
}
