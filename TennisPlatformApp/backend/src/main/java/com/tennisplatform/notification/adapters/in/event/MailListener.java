package com.tennisplatform.notification.adapters.in.event;

import com.tennisplatform.booking.application.port.in.BookingCancelled;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;
import com.tennisplatform.notification.application.port.in.NotifyBookings;
import com.tennisplatform.notification.application.port.in.NotifyMessages;
import com.tennisplatform.notification.configuration.NotificationProperties;
import com.tennisplatform.notification.domain.MessageSent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hears what {@code booking} announces and the teacher's messages (Fase 24), once their
 * transaction has committed, and hands them to {@link NotifyBookings} and {@link NotifyMessages}
 * in the background.
 *
 * <p>After the commit, so a booking that a rollback undid (the loser of the race for the last
 * seat) never gets a confirmation; in the background, so cancelling a rainy day with dozens of
 * students does not hold the teacher's request until every email is out. A fact published with
 * no transaction around it is dropped, which is the default of {@link TransactionalEventListener}
 * and the right one: every booking change runs in one.
 *
 * <p>The queue is not an {@code Executor} bean on purpose: one would replace the executor Spring
 * Boot configures for the rest of the application. If the server stops before an email leaves,
 * the email is lost; the booking is not (accepted in 32-fase20-analisis-avisos-calendario.md).
 */
@Component
class MailListener implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MailListener.class);

    private final NotifyBookings notify;
    private final NotifyMessages messages;
    private final Executor executor;

    MailListener(NotifyBookings notify, NotifyMessages messages, NotificationProperties properties) {
        this.notify = notify;
        this.messages = messages;
        this.executor = properties.isSendInBackground() ? queue() : Runnable::run;
    }

    @TransactionalEventListener
    void on(BookingConfirmed booking) {
        submit("booking " + booking.bookingId() + " confirmed", () -> notify.confirmed(booking));
    }

    @TransactionalEventListener
    void on(BookingCancelled booking) {
        submit("booking " + booking.bookingId() + " cancelled", () -> notify.cancelled(booking));
    }

    @TransactionalEventListener
    void on(MessageSent message) {
        submit("message " + message.messageId(), () -> messages.sent(message.messageId()));
    }

    /**
     * Carries the correlation id over to the sending thread, so the email's log line can be
     * found next to the request that caused it, and never lets a failure escape: it is logged by
     * type only, because its message can quote an address.
     */
    private void submit(String what, Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        executor.execute(() -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                task.run();
            } catch (RuntimeException e) {
                log.error("Could not notify {}: {}", what, e.getClass().getName());
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        });
    }

    /**
     * One thread is plenty for a single school; a full queue makes the caller send it itself
     * rather than drop it.
     */
    private static ThreadPoolTaskExecutor queue() {
        ThreadPoolTaskExecutor queue = new ThreadPoolTaskExecutor();
        queue.setCorePoolSize(1);
        queue.setMaxPoolSize(1);
        queue.setQueueCapacity(500);
        queue.setThreadNamePrefix("mail-");
        queue.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        queue.setWaitForTasksToCompleteOnShutdown(true);
        queue.setAwaitTerminationSeconds(20);
        queue.initialize();
        return queue;
    }

    @Override
    public void destroy() {
        if (executor instanceof ThreadPoolTaskExecutor queue) {
            queue.shutdown();
        }
    }
}
