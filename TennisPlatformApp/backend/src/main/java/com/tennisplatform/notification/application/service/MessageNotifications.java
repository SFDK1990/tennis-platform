package com.tennisplatform.notification.application.service;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.notification.application.port.in.NotifyMessages;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.notification.application.port.out.MessageRepository;
import com.tennisplatform.notification.domain.Message;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Emails a message to the recipients who can read it there: an account that is active and whose
 * address was verified. A deleted account keeps a made-up address, a disabled one is somebody the
 * school stopped writing to, and an unverified address may not even be theirs.
 */
public class MessageNotifications implements NotifyMessages {

    private static final String ACTIVE = "ACTIVE";

    private final Mailer mailer;
    private final MailTexts texts;
    private final MessageRepository messages;
    private final FindUserAccounts accounts;
    private final GetStudentProfile students;
    private final GetTeacherProfile teachers;

    public MessageNotifications(Mailer mailer, MailTexts texts, MessageRepository messages, FindUserAccounts accounts,
                                GetStudentProfile students, GetTeacherProfile teachers) {
        this.mailer = mailer;
        this.texts = texts;
        this.messages = messages;
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
    }

    /** Read here, after the commit: a message deleted in between reaches nobody. */
    @Override
    @Transactional(readOnly = true)
    public void sent(UUID messageId) {
        Optional<Message> found = messages.findById(messageId);
        if (found.isEmpty()) {
            return;
        }
        Message message = found.get();
        String teacherName = teachers.byUserId(message.teacherUserId()).map(TeacherProfileView::displayName)
                .orElse("Tu profesor");
        List<UUID> recipients = messages.recipientsOf(messageId);
        Map<UUID, UserSummary> accountsById = accounts.byIds(recipients);
        for (UUID studentUserId : recipients) {
            UserSummary account = accountsById.get(studentUserId);
            if (account == null || !ACTIVE.equals(account.status()) || account.emailVerifiedAt() == null) {
                continue;
            }
            String name = students.byUserId(studentUserId).map(StudentProfileView::fullName).orElse(null);
            mailer.send(texts.messageToStudent(messageId, message.subject(), message.body(), teacherName,
                    account.email(), name));
        }
    }
}
