package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.student.application.port.in.ManagedStudentDetailView;
import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.student.application.port.in.ManagedStudentView;
import com.tennisplatform.student.application.port.in.StudentLookupView;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Wire representations. Kept apart from the domain so persistence can never leak out. */
final class StudentDtos {

    private StudentDtos() {
    }

    /**
     * One row of the teacher's list. It has no field for the national id or the address, which
     * is what makes it impossible to leak them in a listing by mistake
     * (02-arquitectura.md).
     */
    /** {@code levelCompleted}: every objective of their level is achieved (Fase 29). */
    record StudentSummaryResponse(UUID userId, String fullName, String email,
                                  String managedStatus, Instant managedAt, boolean hasPhoto,
                                  boolean levelCompleted) {

        static StudentSummaryResponse from(ManagedStudentView view, Set<UUID> withPhoto, Set<UUID> completed) {
            return new StudentSummaryResponse(view.userId(), view.fullName(), view.email(),
                    view.managedStatus(), view.managedAt(), withPhoto.contains(view.userId()),
                    completed.contains(view.userId()));
        }
    }

    record StudentSummaryPageResponse(List<StudentSummaryResponse> items, int page, int size,
                                      long totalItems) {

        static StudentSummaryPageResponse from(ResultPage<ManagedStudentView> page, Set<UUID> withPhoto,
                                               Set<UUID> completed) {
            return new StudentSummaryPageResponse(
                    page.items().stream().map(view -> StudentSummaryResponse.from(view, withPhoto, completed)).toList(),
                    page.page(), page.size(), page.totalItems());
        }
    }

    /** The answer to a look-up by exact email: enough to confirm the person, nothing more. */
    record StudentLookupResponse(UUID userId, String email, String fullName,
                                 String managedStatus) {

        static StudentLookupResponse from(StudentLookupView view) {
            return new StudentLookupResponse(view.userId(), view.email(), view.fullName(),
                    view.managedStatus());
        }
    }

    /** The detail of one managed student, only ever built for the teacher managing them. */
    record StudentDetailResponse(UUID userId, String email, String fullName, String phone,
                                 String nationalId, String address, String managedStatus,
                                 Instant managedAt, Instant deactivatedAt, UUID levelId, String goals,
                                 String privateNotes, boolean hasPhoto) {

        static StudentDetailResponse from(ManagedStudentDetailView view, Set<UUID> withPhoto) {
            return new StudentDetailResponse(view.userId(), view.email(), view.fullName(),
                    view.phone(), view.nationalId(), view.address(), view.managedStatus(),
                    view.managedAt(), view.deactivatedAt(), view.levelId(), view.goals(), view.privateNotes(),
                    withPhoto.contains(view.userId()));
        }
    }

    /** The teacher's record of the student, replaced whole: a null level means none (Fase 23). */
    record StudentRecordRequest(UUID levelId, @Size(max = 2000) String goals, @Size(max = 2000) String privateNotes) {
    }
}
