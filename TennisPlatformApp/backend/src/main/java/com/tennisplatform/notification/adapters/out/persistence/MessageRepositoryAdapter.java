package com.tennisplatform.notification.adapters.out.persistence;

import com.tennisplatform.notification.application.port.out.MessageRepository;
import com.tennisplatform.notification.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository messages;
    private final MessageRecipientJpaRepository recipients;

    MessageRepositoryAdapter(MessageJpaRepository messages, MessageRecipientJpaRepository recipients) {
        this.messages = messages;
        this.recipients = recipients;
    }

    @Override
    public Message save(Message message, Collection<UUID> students) {
        Message saved = messages.saveAndFlush(MessageEntity.fromDomain(message)).toDomain();
        recipients.saveAll(students.stream().map(student -> new MessageRecipientEntity(saved.id(), student)).toList());
        recipients.flush();
        return saved;
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return messages.findById(id).map(MessageEntity::toDomain);
    }

    @Override
    public List<UUID> recipientsOf(UUID messageId) {
        return recipients.studentsOf(messageId);
    }

    @Override
    public Slice findSentBy(UUID teacherUserId, int page, int size) {
        return slice(messages.findAllByTeacherUserIdOrderBySentAtDescIdDesc(teacherUserId, PageRequest.of(page, size)));
    }

    @Override
    public Slice findReceivedBy(UUID studentUserId, int page, int size) {
        return slice(messages.findReceivedBy(studentUserId, PageRequest.of(page, size)));
    }

    /** The foreign key cascades to the recipients. */
    @Override
    public void delete(UUID id) {
        messages.deleteById(id);
        messages.flush();
    }

    @Override
    public void forgetRecipient(UUID studentUserId) {
        recipients.deleteAllOfStudent(studentUserId);
    }

    private static Slice slice(Page<MessageEntity> found) {
        return new Slice(found.getContent().stream().map(MessageEntity::toDomain).toList(), found.getTotalElements());
    }
}
