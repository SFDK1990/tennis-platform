package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.shared.domain.ResultPage;

import java.util.List;
import java.util.UUID;

/** Reading the messages, newest first: the teacher's sent ones and each student's board. */
public interface GetMessages {

    ResultPage<SentMessageView> sentBy(UUID teacherUserId, int page, int size);

    /** Only what was sent to this user; anybody else's board is not reachable from here. */
    ResultPage<ReceivedMessageView> receivedBy(UUID userId, int page, int size);

    /** Everything on the student's board, for their export. */
    List<ReceivedMessageView> allReceivedBy(UUID userId);
}
