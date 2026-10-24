package com.tennisplatform.booking.adapters.in.web;

import com.tennisplatform.booking.adapters.in.web.BookingDtos.LessonSummary;
import com.tennisplatform.booking.application.port.in.WaitingList;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The waiting list of a full lesson (Fase 21): the student joins and leaves, the teacher reads it. */
@RestController
@RequestMapping("/api/v1")
class WaitingListController {

    private final WaitingList waitingList;

    WaitingListController(WaitingList waitingList) {
        this.waitingList = waitingList;
    }

    @PostMapping("/lessons/{id}/waiting-list")
    @ResponseStatus(HttpStatus.CREATED)
    WaitingPlaceResponse join(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireStudent(caller);
        return WaitingPlaceResponse.from(waitingList.join(caller.id(), caller.emailVerified(), id));
    }

    @DeleteMapping("/lessons/{id}/waiting-list")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireStudent(caller);
        waitingList.leave(caller.id(), id);
    }

    @GetMapping("/waiting-list")
    WaitingPlacesResponse mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        requireStudent(caller);
        return new WaitingPlacesResponse(waitingList.forStudent(caller.id()).stream()
                .map(WaitingPlaceResponse::from)
                .toList());
    }

    @GetMapping("/teacher/lessons/{id}/waiting-list")
    WaitingStudentsResponse ofLesson(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.roleNotAllowed("Only the teacher reads a lesson's waiting list");
        }
        return new WaitingStudentsResponse(waitingList.forTeacher(caller.id(), id).stream()
                .map(student -> new WaitingStudentResponse(student.studentUserId(), student.position(),
                        student.joinedAt()))
                .toList());
    }

    private static void requireStudent(AuthenticatedUser caller) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student waits for a seat");
        }
    }

    record WaitingPlaceResponse(LessonSummary lesson, int position, Instant joinedAt) {

        static WaitingPlaceResponse from(WaitingList.WaitingPlace place) {
            return new WaitingPlaceResponse(LessonSummary.from(place.lesson()), place.position(), place.joinedAt());
        }
    }

    record WaitingPlacesResponse(List<WaitingPlaceResponse> items) {
    }

    record WaitingStudentResponse(UUID studentUserId, int position, Instant joinedAt) {
    }

    record WaitingStudentsResponse(List<WaitingStudentResponse> items) {
    }
}
