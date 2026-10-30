package com.tennisplatform.pass;

import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
 * The criterion of 41-fase27-analisis-bonos.md: one student, one lesson left, two bookings of two
 * different lessons at the same moment. The lessons' locks do not serialise them, since they are
 * different lessons; the lock on the student's passes does. Exactly one is booked, and the pass is
 * never spent twice.
 */
class PassBalanceConcurrencyTest extends AbstractPassTest {

    private static final int ROUNDS = 5;

    @Test
    @SuppressWarnings("rawtypes")
    void twoBookingsAtOnceWithOneLessonLeftBookOnlyOne() throws Exception {
        requirePasses(true);
        UUID oneLesson = aPassType("Una", "GROUP", 1, null);
        for (int round = 0; round < ROUNDS; round++) {
            Student student = aStudentWhoMayBook();
            UUID pass = aPassOf(student, oneLesson);
            UUID first = aLessonStartingIn(Duration.ofDays(30).plusHours(4L * round), LessonType.GROUP, 4);
            UUID second = aLessonStartingIn(Duration.ofDays(30).plusHours(4L * round + 2), LessonType.GROUP, 4);

            List<ResponseEntity<Map>> answers = simultaneously(
                    () -> book(student.token(), first), () -> book(student.token(), second));

            assertThat(answers).extracting(ResponseEntity::getStatusCode)
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(answers).filteredOn(answer -> answer.getStatusCode() == HttpStatus.UNPROCESSABLE_CONTENT)
                    .singleElement()
                    .satisfies(answer -> assertThat(answer.getBody()).containsEntry("code", "NO_PASS_BALANCE"));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings WHERE pass_id = ? AND status = 'CONFIRMED'",
                    Integer.class, pass)).isEqualTo(1);
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
