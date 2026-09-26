package com.tennisplatform.student.application.service;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.student.application.port.in.GetManagedStudents;
import com.tennisplatform.student.application.port.in.ManagedStudentDetailView;
import com.tennisplatform.shared.domain.ResultPage;
import com.tennisplatform.student.application.port.in.ManagedStudentView;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.student.application.port.in.StudentLookupView;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.application.port.out.StudentProfileRepository;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.student.domain.StudentProfile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The teacher's read side: the list of their students, the detail of one of them, and the
 * look-up by exact email that precedes managing somebody.
 *
 * <p>A listing needs three things that live in three places - the relationship here, the name
 * in {@code student_profiles}, the address in {@code users} - and the module boundaries forbid
 * joining across them in SQL. So the rows are fetched per source and joined in memory, in bulk,
 * never one query per row. What makes that acceptable rather than a hidden scalability problem
 * is the platform's own student limit: the teacher's whole set is bounded by configuration, and
 * this service reads that bounded set. If the limit ever grows into the thousands, this is the
 * place that has to change - and it will change here, not everywhere.
 */
public class GetManagedStudentsService implements GetManagedStudents, QueryManagedStudent {

    private final ManagedStudentRepository relationships;
    private final StudentProfileRepository profiles;
    private final FindUserAccounts accounts;

    public GetManagedStudentsService(ManagedStudentRepository relationships,
                                     StudentProfileRepository profiles,
                                     FindUserAccounts accounts) {
        this.relationships = relationships;
        this.profiles = profiles;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isManagedBy(UUID teacherUserId, UUID studentUserId) {
        return relationships.findByPair(teacherUserId, studentUserId)
                .map(ManagedStudent::isManaged)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<ManagedStudentView> list(UUID teacherUserId, String query, int page, int size) {
        List<ManagedStudent> managed = relationships.findAllByTeacher(teacherUserId).stream()
                .filter(ManagedStudent::isManaged)
                .toList();

        List<UUID> studentIds = managed.stream().map(ManagedStudent::studentUserId).toList();
        Map<UUID, StudentProfile> profilesById = profilesOf(studentIds);
        Map<UUID, UserSummary> accountsById = accounts.byIds(studentIds);

        List<ManagedStudentView> matching = managed.stream()
                .map(relationship -> toView(relationship, profilesById, accountsById))
                .filter(view -> matches(view, query))
                .toList();

        List<ManagedStudentView> items = matching.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        return new ResultPage<ManagedStudentView>(items, page, size, matching.size());
    }

    /**
     * Deliberately not restricted to the teacher's own students: its whole purpose is finding
     * somebody they do not manage yet. What keeps it from becoming a directory of the platform
     * is that it only answers to a complete address, and answers with a name and nothing else.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<StudentLookupView> lookupByEmail(UUID teacherUserId, String email) {
        return accounts.byEmail(email)
                .filter(account -> "STUDENT".equals(account.role()))
                .map(account -> new StudentLookupView(account.id(), account.email(),
                        profiles.findByUserId(account.id())
                                .map(StudentProfile::fullName)
                                .orElse(null),
                        relationships.findByPair(teacherUserId, account.id())
                                .map(relationship -> relationship.status().name())
                                .orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedStudentDetailView detail(UUID teacherUserId, UUID studentUserId) {
        ManagedStudent relationship = relationships.findByPair(teacherUserId, studentUserId)
                .orElseThrow(() -> new StudentNotManagedException(
                        "This student is not managed by this teacher"));

        StudentProfile profile = profiles.findByUserId(studentUserId)
                .orElseThrow(() -> new StudentNotManagedException(
                        "This student has no profile to show"));

        UserSummary account = accounts.byIds(List.of(studentUserId)).get(studentUserId);

        return new ManagedStudentDetailView(studentUserId,
                account == null ? null : account.email(), profile.fullName(), profile.phone(),
                profile.nationalId(), profile.address(), relationship.status().name(),
                relationship.managedAt(), relationship.deactivatedAt());
    }

    // --- helpers ---------------------------------------------------------------

    private Map<UUID, StudentProfile> profilesOf(List<UUID> studentIds) {
        return profiles.findAllByUserIds(studentIds).stream()
                .collect(Collectors.toMap(StudentProfile::userId, Function.identity()));
    }

    private ManagedStudentView toView(ManagedStudent relationship,
                                      Map<UUID, StudentProfile> profilesById,
                                      Map<UUID, UserSummary> accountsById) {
        UUID studentId = relationship.studentUserId();
        StudentProfile profile = profilesById.get(studentId);
        UserSummary account = accountsById.get(studentId);

        return new ManagedStudentView(studentId, account == null ? null : account.email(),
                profile == null ? null : profile.fullName(), relationship.status().name(),
                relationship.managedAt());
    }

    /**
     * Partial, case insensitive, over name and email - and safe to be partial because it only
     * ever runs over students this teacher already manages.
     */
    private boolean matches(ManagedStudentView view, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return contains(view.fullName(), needle) || contains(view.email(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
