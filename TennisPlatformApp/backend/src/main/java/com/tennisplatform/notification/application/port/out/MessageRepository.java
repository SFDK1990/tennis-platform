package com.tennisplatform.notification.application.port.out;

import com.tennisplatform.notification.domain.Message;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {

    /** Saves a new message together with the students it went to. */
    Message save(Message message, Collection<UUID> recipients);

    Optional<Message> findById(UUID id);

    List<UUID> recipientsOf(UUID messageId);

    Slice findSentBy(UUID teacherUserId, int page, int size);

    Slice findReceivedBy(UUID studentUserId, int page, int size);

    /** Its recipients go with it. */
    void delete(UUID id);

    void forgetRecipient(UUID studentUserId);

    record Slice(List<Message> items, long total) {

        public Slice {
            items = List.copyOf(items);
        }
    }
}
