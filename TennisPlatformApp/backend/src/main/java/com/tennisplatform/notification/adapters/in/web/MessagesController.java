package com.tennisplatform.notification.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.notification.application.port.in.GetMessages;
import com.tennisplatform.notification.application.port.in.ManageMessages;
import com.tennisplatform.notification.application.port.in.ManageMessages.MessageDraft;
import com.tennisplatform.notification.application.port.in.ReceivedMessageView;
import com.tennisplatform.notification.application.port.in.SentMessageView;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.shared.domain.ResultPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The teacher's messages and the board of whoever is signed in (Fase 24). */
@RestController
class MessagesController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ManageMessages manage;
    private final GetMessages get;

    MessagesController(ManageMessages manage, GetMessages get) {
        this.manage = manage;
        this.get = get;
    }

    @PostMapping("/api/v1/teacher/messages")
    @ResponseStatus(HttpStatus.CREATED)
    SentMessageView send(@AuthenticationPrincipal AuthenticatedUser caller,
                         @Valid @RequestBody SendMessageRequest request) {
        requireTeacher(caller);
        return manage.send(caller.id(), new MessageDraft(request.audience(), request.levelId(), request.lessonId(),
                request.subject(), request.body()));
    }

    @GetMapping("/api/v1/teacher/messages")
    ResultPage<SentMessageView> sent(@AuthenticationPrincipal AuthenticatedUser caller,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        requireTeacher(caller);
        return get.sentBy(caller.id(), Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @DeleteMapping("/api/v1/teacher/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireTeacher(caller);
        manage.delete(caller.id(), id);
    }

    /** Everybody signed in has a board; only students ever receive anything on it. */
    @GetMapping("/api/v1/messages")
    ResultPage<ReceivedMessageView> received(@AuthenticationPrincipal AuthenticatedUser caller,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return get.receivedBy(caller.id(), Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    private static void requireTeacher(AuthenticatedUser caller) {
        if (caller == null || !caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher writes messages");
        }
    }

    /** The lengths are the domain's to check, so a long text gets one clear message. */
    record SendMessageRequest(@NotNull String audience, UUID levelId, UUID lessonId, @NotNull String subject,
                              @NotNull String body) {
    }
}
