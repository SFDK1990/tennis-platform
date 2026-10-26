package com.tennisplatform.notification.adapters.out.persistence;

import com.tennisplatform.notification.domain.Message;
import com.tennisplatform.notification.domain.MessageAudience;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
class MessageEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "teacher_user_id", nullable = false, updatable = false)
    private UUID teacherUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private MessageAudience audience;

    @Column(name = "audience_label", nullable = false, updatable = false, length = 120)
    private String audienceLabel;

    @Column(nullable = false, updatable = false, length = Message.MAX_SUBJECT)
    private String subject;

    @Column(nullable = false, updatable = false, length = Message.MAX_BODY)
    private String body;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "recipient_count", nullable = false, updatable = false)
    private int recipientCount;

    protected MessageEntity() {
    }

    static MessageEntity fromDomain(Message message) {
        MessageEntity entity = new MessageEntity();
        entity.id = message.id();
        entity.teacherUserId = message.teacherUserId();
        entity.audience = message.audience();
        entity.audienceLabel = message.audienceLabel();
        entity.subject = message.subject();
        entity.body = message.body();
        entity.sentAt = message.sentAt();
        entity.recipientCount = message.recipientCount();
        return entity;
    }

    Message toDomain() {
        return new Message(id, teacherUserId, audience, audienceLabel, subject, body, sentAt, recipientCount);
    }
}
