package com.tennisplatform.notification.adapters.in.event;

import com.tennisplatform.booking.application.port.in.BookingCancelled;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;
import com.tennisplatform.notification.application.port.in.NotifyBookings;
import com.tennisplatform.notification.configuration.NotificationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** The background queue, which the test profile replaces by the request's own thread. */
@ExtendWith(OutputCaptureExtension.class)
class BookingFactsListenerTest {

    private static final Instant STARTS = Instant.parse("2026-11-03T09:00:00Z");

    private BookingFactsListener listener;

    @AfterEach
    void stop() {
        MDC.clear();
        listener.destroy();
    }

    private static BookingFactsListener inTheBackground(NotifyBookings notify) {
        NotificationProperties properties = new NotificationProperties();
        properties.setSendInBackground(true);
        return new BookingFactsListener(notify, properties);
    }

    private static BookingConfirmed confirmed() {
        return new BookingConfirmed(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                STARTS, STARTS.plusSeconds(3600), false);
    }

    @Test
    void theEmailLeavesFromAnotherThreadCarryingTheRequestsCorrelationId() throws Exception {
        CompletableFuture<String> seen = new CompletableFuture<>();
        listener = inTheBackground(new NotifyBookings() {
            @Override
            public void confirmed(BookingConfirmed booking) {
                seen.complete(Thread.currentThread().getName() + " " + MDC.get("correlationId"));
            }

            @Override
            public void cancelled(BookingCancelled booking) {
            }
        });
        MDC.put("correlationId", "request-42");

        listener.on(confirmed());

        assertThat(seen.get(5, TimeUnit.SECONDS)).startsWith("mail-").endsWith(" request-42");
    }

    @Test
    void aFailureIsLoggedByTypeAndGoesNoFurther(CapturedOutput output) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        listener = inTheBackground(new NotifyBookings() {
            @Override
            public void confirmed(BookingConfirmed booking) {
            }

            @Override
            public void cancelled(BookingCancelled booking) {
                done.complete(null);
                throw new IllegalStateException("no address for lucia@example.com");
            }
        });
        UUID bookingId = UUID.randomUUID();

        listener.on(new BookingCancelled(bookingId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), STARTS,
                STARTS.plusSeconds(3600), "CANCELLED_BY_TEACHER", null));
        done.get(5, TimeUnit.SECONDS);
        listener.destroy();

        assertThat(output.getAll()).contains("Could not notify booking " + bookingId + " cancelled: "
                + IllegalStateException.class.getName()).doesNotContain("lucia@example.com");
    }
}
