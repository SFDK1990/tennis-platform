package com.tennisplatform.booking;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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

    /**
     * The teacher lowers the capacity to one while two students book (30-fase19-analisis-cierre-mvp.md).
     * Whatever order the lock lets them through in, the lesson never ends with more confirmed
     * bookings than seats, and nobody gets a 500. Without the lock in the edit, the capacity was
     * written from a count taken before the bookings, and two students sat in a lesson of one.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void loweringTheCapacityWhileStudentsBookNeverLeavesMoreBookingsThanSeats() throws Exception {
        for (int round = 0; round < ROUNDS; round++) {
            UUID lessonId = aLessonStartingIn(Duration.ofDays(40).plusHours(2L * round),
                    com.tennisplatform.lesson.domain.LessonType.GROUP, 3);
            String first = aStudentWhoMayBook().token();
            String second = aStudentWhoMayBook().token();

            List<ResponseEntity<Map>> answers = simultaneously(
                    () -> rest.exchange("/api/v1/teacher/lessons/" + lessonId, HttpMethod.PATCH,
                            new HttpEntity<>(Map.of("capacity", 1), jsonBearer(teacherToken)), Map.class),
                    () -> book(first, lessonId), () -> book(second, lessonId));

            assertThat(answers).allSatisfy(answer -> assertThat(answer.getStatusCode().is5xxServerError()).isFalse());
            Integer capacity = jdbc.queryForObject("SELECT capacity FROM lessons WHERE id = ?", Integer.class, lessonId);
            assertThat(confirmedBookingsOf(lessonId)).isLessThanOrEqualTo(capacity);
        }
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
