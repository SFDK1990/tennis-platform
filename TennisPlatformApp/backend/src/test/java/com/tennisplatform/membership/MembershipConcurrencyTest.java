package com.tennisplatform.membership;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two students joining a group with one seat left, at the same moment. The lesson's lock serialises
 * them as it does two bookings of the last seat, so one gets in and the other hears the group is full.
 */
class MembershipConcurrencyTest extends AbstractMembershipTest {

    @Test
    @SuppressWarnings("rawtypes")
    void twoStudentsRaceForTheLastSeatOfAGroupAndExactlyOneJoins() throws Exception {
        Series series = aWeeklySeries(1, Duration.ofDays(2), 3);
        UUID group = aGroupOf(series);
        Student one = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        CountDownLatch start = new CountDownLatch(1);

        CompletableFuture<ResponseEntity<Map>> first = CompletableFuture.supplyAsync(() -> joinWhen(start, one, group));
        CompletableFuture<ResponseEntity<Map>> second = CompletableFuture.supplyAsync(() -> joinWhen(start, other, group));
        start.countDown();
        List<HttpStatus> statuses = List.of(HttpStatus.valueOf(first.get().getStatusCode().value()),
                HttpStatus.valueOf(second.get().getStatusCode().value()));

        assertThat(statuses).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        for (UUID lesson : series.lessonIds()) {
            assertThat(confirmedBookingsOf(lesson)).isEqualTo(1);
        }
        assertThat(groupAsTheTeacher(group)).containsEntry("memberCount", 1);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> joinWhen(CountDownLatch start, Student student, UUID group) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return join(student, group);
    }
}
