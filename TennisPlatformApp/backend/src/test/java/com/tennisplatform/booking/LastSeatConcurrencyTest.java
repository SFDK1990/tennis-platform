package com.tennisplatform.booking;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The exit criterion of Fase 9: two students booking the last seat at the same moment, and
 * exactly one of them getting it.
 *
 * <p>Both requests are released by the same latch, so they reach the server together rather than
 * one after the other. A race won once proves little - the losing request may simply have been
 * slower - so it is run on several lessons in the same test. What must hold every time: one 201,
 * one 409 {@code LESSON_FULL}, no 500, and a single confirmed row in the table.
 */
class LastSeatConcurrencyTest extends AbstractBookingTest {

    private static final int ROUNDS = 5;

    @Autowired
    private DataSource dataSource;

    /**
     * With a single database connection the two requests would queue for the pool instead of for
     * the lesson's lock, and the test would pass without testing anything. Checked rather than
     * assumed, as 20-fase9-analisis-booking.md asks.
     */
    @Test
    void thePoolHasRoomForTwoRequestsAtOnce() {
        assertThat(((HikariDataSource) dataSource).getMaximumPoolSize()).isGreaterThanOrEqualTo(2);
    }

    /** Exit criterion of Fase 9. */
    @Test
    @SuppressWarnings("rawtypes")
    void twoStudentsRaceForTheLastSeatAndExactlyOneWins() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID lessonId = anIndividualLessonIn(Duration.ofDays(30).plusHours(2L * round));
            String first = aStudentWhoMayBook().token();
            String second = aStudentWhoMayBook().token();

            List<ResponseEntity<Map>> answers = simultaneously(
                    () -> book(first, lessonId), () -> book(second, lessonId));

            assertThat(answers).extracting(ResponseEntity::getStatusCode)
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
            assertThat(answers).filteredOn(answer -> answer.getStatusCode() == HttpStatus.CONFLICT)
                    .singleElement()
                    .satisfies(answer -> assertThat(answer.getBody()).containsEntry("code", "LESSON_FULL"));
            assertThat(confirmedBookingsOf(lessonId)).isEqualTo(1);
        }
    }

    /**
     * The same student pressing the button twice. The lock serialises the two, so the second sees
     * the first's row and answers that the seat is already theirs - never a second seat and never
     * a 500 from the unique index.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void theSameStudentSendingTwoBookingsAtOnceGetsOneSeat() throws Exception {
        UUID lessonId = aLessonStartingIn(Duration.ofDays(30), com.tennisplatform.lesson.domain.LessonType.GROUP, 4);
        String token = aStudentWhoMayBook().token();

        List<ResponseEntity<Map>> answers = simultaneously(
                () -> book(token, lessonId), () -> book(token, lessonId));

        assertThat(answers).extracting(ResponseEntity::getStatusCode)
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(confirmedBookingsOf(lessonId)).isEqualTo(1);
    }

    @SafeVarargs
    @SuppressWarnings("rawtypes")
    private static List<ResponseEntity<Map>> simultaneously(Callable<ResponseEntity<Map>>... requests)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests.length);
        try {
            CountDownLatch ready = new CountDownLatch(requests.length);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
            for (Callable<ResponseEntity<Map>> request : requests) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return request.call();
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();

            List<ResponseEntity<Map>> answers = new ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                answers.add(future.get(30, TimeUnit.SECONDS));
            }
            return answers;
        } finally {
            pool.shutdownNow();
        }
    }
}
