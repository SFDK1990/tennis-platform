package com.tennisplatform.student.application.port.in;

import com.tennisplatform.shared.domain.ResultPage;

import java.util.Optional;
import java.util.UUID;

/**
 * The teacher's view of their students: the list, the detail, and the look-up that precedes
 * managing somebody.
 *
 * <p>The three are deliberately different shapes. 16-fase6-analisis-perfiles.md splits the two
 * jobs the original contract gave to a single {@code ?query=} parameter: searching <em>among
 * my own students</em>, which may be partial, and searching <em>the whole platform</em> to
 * manage somebody, which may not - a partial search over every account lets a teacher
 * enumerate who is registered and read the names of people they have no relationship with.
 */
public interface GetManagedStudents {

    /**
     * The students this teacher manages, optionally narrowed by a partial match on name or
     * email. The partial match is safe here precisely because it never leaves the set of
     * students the teacher already manages.
     */
    ResultPage<ManagedStudentView> list(UUID teacherUserId, String query, int page, int size);

    /**
     * The account with this exact address, if it exists and belongs to a student.
     *
     * <p>Exact and complete: no prefixes, no wildcards, no partial matches. Somebody about to
     * manage a student knows their address and does not need to explore.
     *
     * <p>The teacher id is needed even though the search is not restricted to their students:
     * it is what turns the answer into "already managed" or "can be managed", which is the
     * whole reason the caller is asking.
     */
    Optional<StudentLookupView> lookupByEmail(UUID teacherUserId, String email);

    /**
     * The full record of one student, restricted personal data included.
     *
     * <p>Only for the teacher who manages them: any other caller gets a refusal, because
     * authorization here is by relationship and not only by role (08-security-engineer.md).
     * A deactivated student is still readable by the teacher who managed them - the data did
     * not stop being theirs to see when the lessons stopped.
     */
    ManagedStudentDetailView detail(UUID teacherUserId, UUID studentUserId);
}
