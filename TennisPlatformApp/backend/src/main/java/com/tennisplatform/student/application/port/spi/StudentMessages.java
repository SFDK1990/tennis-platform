package com.tennisplatform.student.application.port.spi;

import java.util.UUID;

/**
 * What deleting an account needs done to the teacher's messages, which only {@code notification}
 * can do (Fase 24). Runs inside the caller's transaction.
 */
public interface StudentMessages {

    /** The student stops being a recipient of every message; the messages stay for the others. */
    void forgetRecipient(UUID studentUserId);
}
