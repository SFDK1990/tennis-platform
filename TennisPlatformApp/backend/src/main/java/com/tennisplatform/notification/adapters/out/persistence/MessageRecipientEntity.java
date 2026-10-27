package com.tennisplatform.notification.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

/** One student a message went to. */
@Entity
@Table(name = "message_recipients")
class MessageRecipientEntity {

    @EmbeddedId
    private Key id;

    protected MessageRecipientEntity() {
    }

    MessageRecipientEntity(UUID messageId, UUID studentUserId) {
        this.id = new Key(messageId, studentUserId);
    }

    UUID studentUserId() {
        return id.studentUserId;
    }

    @Embeddable
    record Key(@Column(name = "message_id") UUID messageId,
               @Column(name = "student_user_id") UUID studentUserId) implements Serializable {
    }
}
