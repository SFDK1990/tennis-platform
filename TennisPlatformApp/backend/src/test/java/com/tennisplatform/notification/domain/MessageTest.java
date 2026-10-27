package com.tennisplatform.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    private static Message message(String subject, String body) {
        return new Message(null, UUID.randomUUID(), MessageAudience.ALL, "Todos los alumnos", subject, body,
                Instant.parse("2026-11-02T10:00:00Z"), 1);
    }

    @Test
    void theSubjectAndTheTextAreTrimmedAndBothAreRequired() {
        assertThat(message("  Aviso ", " Texto\n").subject()).isEqualTo("Aviso");
        assertThatThrownBy(() -> message(null, "Texto")).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> message("Aviso", "  ")).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> message("x".repeat(Message.MAX_SUBJECT + 1), "Texto"))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void theAudienceIsReadWhateverItsCaseAndAnythingElseIsRefused() {
        assertThat(MessageAudience.of(" series ")).isEqualTo(MessageAudience.SERIES);
        assertThatThrownBy(() -> MessageAudience.of(null)).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> MessageAudience.of(" ")).isInstanceOf(InvalidMessageException.class);
        assertThatThrownBy(() -> MessageAudience.of("GROUP")).isInstanceOf(InvalidMessageException.class);
    }
}
