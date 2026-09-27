package com.tennisplatform.error;

/**
 * The trace of an unexpected failure, with the type of each exception and every frame but none
 * of their messages. A message can quote what the database was given - "(email)=(...)" - and the
 * log keeps no personal data (28-fase16-analisis-observabilidad.md). The type and the frames say
 * what failed and where; the correlation id says which request it was.
 */
final class WithoutMessage extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** A cause chain that loops back on itself must not recurse forever. */
    private static final int MAX_CAUSES = 20;

    private WithoutMessage(Throwable original, int depth) {
        super(original.getClass().getName(),
                original.getCause() == null || depth >= MAX_CAUSES
                        ? null
                        : new WithoutMessage(original.getCause(), depth + 1),
                false, true);
        setStackTrace(original.getStackTrace());
    }

    static WithoutMessage of(Throwable original) {
        return new WithoutMessage(original, 0);
    }
}
