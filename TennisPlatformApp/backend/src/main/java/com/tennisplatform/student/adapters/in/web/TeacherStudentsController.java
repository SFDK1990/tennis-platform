package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentDetailResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentLookupResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentSummaryPageResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentSummaryResponse;
import com.tennisplatform.student.application.port.in.GetManagedStudents;
import com.tennisplatform.student.application.port.in.ManageStudent;
import com.tennisplatform.student.application.port.in.ManagedStudentDetailView;
import com.tennisplatform.student.application.port.in.ManagedStudentView;
import com.tennisplatform.student.application.port.in.StudentPhotos;
import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.student.domain.StudentProfileNotFoundException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentRecordRequest;
import com.tennisplatform.student.application.port.in.UpdateStudentRecord;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The teacher's students.
 *
 * <p>Every endpoint checks the role here and the relationship in the service. Neither check
 * replaces the other: the role says who may ask, the relationship says about whom - and
 * 02-arquitectura.md requires both.
 */
@RestController
@RequestMapping("/api/v1/teacher/students")
class TeacherStudentsController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String MANAGED = "MANAGED";

    private final GetManagedStudents managedStudents;
    private final ManageStudent manageStudent;
    private final UpdateStudentRecord records;
    private final StudentPhotos photos;

    TeacherStudentsController(GetManagedStudents managedStudents, ManageStudent manageStudent,
                              UpdateStudentRecord records, StudentPhotos photos) {
        this.managedStudents = managedStudents;
        this.manageStudent = manageStudent;
        this.records = records;
        this.photos = photos;
    }

    /** The students this teacher manages. {@code query} narrows that set, and only that set. */
    @GetMapping
    StudentSummaryPageResponse list(@AuthenticationPrincipal AuthenticatedUser caller,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        requireTeacher(caller);
        ResultPage<ManagedStudentView> found = managedStudents.list(caller.id(), query, Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE));
        return StudentSummaryPageResponse.from(found,
                photos.withPhoto(found.items().stream().map(ManagedStudentView::userId).toList()));
    }

    /**
     * Finds the account behind an exact address, so the teacher can confirm who they are about
     * to manage. Mapped before {@code /{userId}} by Spring because a literal path wins over a
     * variable one - and even if it did not, "lookup" is not a UUID.
     */
    @GetMapping("/lookup")
    StudentLookupResponse lookup(@AuthenticationPrincipal AuthenticatedUser caller,
                                 @RequestParam String email) {
        requireTeacher(caller);
        return managedStudents.lookupByEmail(caller.id(), email)
                .map(StudentLookupResponse::from)
                .orElseThrow(() -> new StudentProfileNotFoundException(
                        "No student account has that email address"));
    }

    /**
     * The full record of one student, restricted personal data included. The service refuses
     * when this teacher does not manage them, which is what keeps a UUID from being enough to
     * read somebody's address.
     */
    @GetMapping("/{userId}")
    StudentDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser caller,
                                 @PathVariable UUID userId) {
        requireTeacher(caller);
        return detailOf(managedStudents.detail(caller.id(), userId));
    }

    /** Level, goals and private notes, only ever seen by the teacher who wrote them (Fase 23). */
    @PutMapping("/{userId}/record")
    StudentDetailResponse updateRecord(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId,
                                       @Valid @RequestBody StudentRecordRequest request) {
        requireTeacher(caller);
        return detailOf(records.update(caller.id(), userId, request.levelId(), request.goals(),
                request.privateNotes()));
    }

    @PostMapping("/{userId}/manage")
    StudentSummaryResponse manage(@AuthenticationPrincipal AuthenticatedUser caller,
                                  @PathVariable UUID userId) {
        requireTeacher(caller);
        ManagedStudentView managed = manageStudent.manage(caller.id(), userId);
        return StudentSummaryResponse.from(managed, photos.withPhoto(List.of(managed.userId())));
    }

    @DeleteMapping("/{userId}/manage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void stopManaging(@AuthenticationPrincipal AuthenticatedUser caller,
                      @PathVariable UUID userId) {
        requireTeacher(caller);
        manageStudent.stopManaging(caller.id(), userId);
    }

    /** A student the teacher let go keeps their record readable, but not their photo (Fase 25.5). */
    private StudentDetailResponse detailOf(ManagedStudentDetailView view) {
        boolean managed = MANAGED.equals(view.managedStatus());
        return StudentDetailResponse.from(view, managed ? photos.withPhoto(List.of(view.userId())) : Set.of());
    }

    private void requireTeacher(AuthenticatedUser caller) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can manage students");
        }
    }
}
