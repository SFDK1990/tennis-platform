package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentDetailResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentLookupResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentSummaryPageResponse;
import com.tennisplatform.student.adapters.in.web.StudentDtos.StudentSummaryResponse;
import com.tennisplatform.student.application.port.in.GetManagedStudents;
import com.tennisplatform.student.application.port.in.ManageStudent;
import com.tennisplatform.student.domain.StudentProfileNotFoundException;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The teacher's students.
 *
 * <p>Every endpoint checks the role here and the relationship in the service. Neither check
 * replaces the other: the role says who may ask, the relationship says about whom - and
 * 08-security-engineer.md requires both.
 */
@RestController
@RequestMapping("/api/v1/teacher/students")
class TeacherStudentsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final GetManagedStudents managedStudents;
    private final ManageStudent manageStudent;

    TeacherStudentsController(GetManagedStudents managedStudents, ManageStudent manageStudent) {
        this.managedStudents = managedStudents;
        this.manageStudent = manageStudent;
    }

    /** The students this teacher manages. {@code query} narrows that set, and only that set. */
    @GetMapping
    StudentSummaryPageResponse list(@AuthenticationPrincipal AuthenticatedUser caller,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        requireTeacher(caller);
        return StudentSummaryPageResponse.from(managedStudents.list(caller.id(), query,
                Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
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
        return StudentDetailResponse.from(managedStudents.detail(caller.id(), userId));
    }

    @PostMapping("/{userId}/manage")
    StudentSummaryResponse manage(@AuthenticationPrincipal AuthenticatedUser caller,
                                  @PathVariable UUID userId) {
        requireTeacher(caller);
        return StudentSummaryResponse.from(manageStudent.manage(caller.id(), userId));
    }

    @DeleteMapping("/{userId}/manage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void stopManaging(@AuthenticationPrincipal AuthenticatedUser caller,
                      @PathVariable UUID userId) {
        requireTeacher(caller);
        manageStudent.stopManaging(caller.id(), userId);
    }

    private void requireTeacher(AuthenticatedUser caller) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher can manage students");
        }
    }
}
