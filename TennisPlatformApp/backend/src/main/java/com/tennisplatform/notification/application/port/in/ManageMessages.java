package com.tennisplatform.notification.application.port.in;

import java.util.UUID;

/**
 * The teacher writes to their students, or takes a message off the board
 * (37-fase24-analisis-mensajes.md).
 */
public interface ManageMessages {

    /**
     * Works out the recipients now and keeps them with the message: a student who joins the level
     * later does not see it. They get it by email once the transaction commits.
     */
    SentMessageView send(UUID teacherUserId, MessageDraft draft);

    /** The message disappears from every recipient's board; the email cannot be taken back. */
    void delete(UUID teacherUserId, UUID messageId);

    /**
     * What the teacher wrote. {@code levelId} goes only with {@code LEVEL}, {@code lessonId} only
     * with {@code LESSON} and {@code SERIES}.
     */
    record MessageDraft(String audience, UUID levelId, UUID lessonId, String subject, String body) {
    }
}
