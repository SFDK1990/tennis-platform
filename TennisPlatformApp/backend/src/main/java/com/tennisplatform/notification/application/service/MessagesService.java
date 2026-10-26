package com.tennisplatform.notification.application.service;

import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.notification.application.port.in.GetMessages;
import com.tennisplatform.notification.application.port.in.ManageMessages;
import com.tennisplatform.notification.application.port.in.ReceivedMessageView;
import com.tennisplatform.notification.application.port.in.SentMessageView;
import com.tennisplatform.notification.application.port.out.MessageRepository;
import com.tennisplatform.notification.application.port.out.MessageRepository.Slice;
import com.tennisplatform.notification.domain.Message;
import com.tennisplatform.notification.domain.MessageAudience;
import com.tennisplatform.notification.domain.MessageNotFoundException;
import com.tennisplatform.notification.domain.MessageSent;
import com.tennisplatform.notification.domain.NoRecipientsException;
import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.student.application.port.spi.StudentMessages;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The teacher's messages and each student's board (37-fase24-analisis-mensajes.md).
 *
 * <p>The log names the message and how many it reached, never what it says: the text is the
 * teacher's, but it can easily name a student.
 */
public class MessagesService implements ManageMessages, GetMessages, StudentMessages {

    private static final Logger log = LoggerFactory.getLogger(MessagesService.class);
    private static final int EXPORT_PAGE_SIZE = 100;

    private final MessageRepository messages;
    private final Audiences audiences;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public MessagesService(MessageRepository messages, QueryManagedStudent students, QueryLevels levels,
                           GetLesson lessons, GetBookings bookings, GetTeacherProfile teachers,
                           ApplicationEventPublisher events, Clock clock) {
        this.messages = messages;
        this.audiences = new Audiences(students, levels, lessons, bookings, teachers, clock);
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SentMessageView send(UUID teacherUserId, MessageDraft draft) {
        MessageAudience audience = MessageAudience.of(draft.audience());
        Audiences.Resolved resolved = audiences.resolve(teacherUserId, audience, draft.levelId(), draft.lessonId());
        Message message = new Message(null, teacherUserId, audience, resolved.label(), draft.subject(), draft.body(),
                clock.instant(), resolved.recipients().size());
        if (resolved.recipients().isEmpty()) {
            throw new NoRecipientsException("Nobody is in this audience");
        }
        Message saved = messages.save(message, resolved.recipients());
        events.publishEvent(new MessageSent(saved.id()));
        log.info("Message {} sent to {} students", saved.id(), saved.recipientCount());
        return SentMessageView.from(saved);
    }

    @Override
    @Transactional
    public void delete(UUID teacherUserId, UUID messageId) {
        messages.findById(messageId)
                .filter(message -> message.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new MessageNotFoundException("No message with id " + messageId));
        messages.delete(messageId);
        log.info("Message {} deleted", messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<SentMessageView> sentBy(UUID teacherUserId, int page, int size) {
        Slice slice = messages.findSentBy(teacherUserId, page, size);
        return new ResultPage<>(slice.items().stream().map(SentMessageView::from).toList(), page, size, slice.total());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ReceivedMessageView> receivedBy(UUID userId, int page, int size) {
        Slice slice = messages.findReceivedBy(userId, page, size);
        return new ResultPage<>(slice.items().stream().map(ReceivedMessageView::from).toList(), page, size,
                slice.total());
    }

    /** Read in pages, like the export of the bookings. */
    @Override
    @Transactional(readOnly = true)
    public List<ReceivedMessageView> allReceivedBy(UUID userId) {
        List<ReceivedMessageView> all = new ArrayList<>();
        for (int page = 0; ; page++) {
            Slice slice = messages.findReceivedBy(userId, page, EXPORT_PAGE_SIZE);
            slice.items().stream().map(ReceivedMessageView::from).forEach(all::add);
            if (slice.items().size() < EXPORT_PAGE_SIZE || all.size() >= slice.total()) {
                return all;
            }
        }
    }

    /** Inside the account deletion's transaction (Fase 24, decision 6). */
    @Override
    public void forgetRecipient(UUID studentUserId) {
        messages.forgetRecipient(studentUserId);
    }
}
