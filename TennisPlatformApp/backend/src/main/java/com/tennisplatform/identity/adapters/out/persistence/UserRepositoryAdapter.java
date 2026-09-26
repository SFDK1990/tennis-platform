package com.tennisplatform.identity.adapters.out.persistence;

import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.UserStatus;
import com.tennisplatform.shared.domain.ResultPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private static final char LIKE_ESCAPE = '\\';
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return jpa.findByEmail(email.value()).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(UserEntity::toDomain).toList();
    }

    @Override
    public boolean teacherExists() {
        return jpa.existsByRole(Role.TEACHER);
    }

    @Override
    public Optional<User> findTheTeacher() {
        return jpa.findFirstByRole(Role.TEACHER).map(UserEntity::toDomain);
    }

    @Override
    public ResultPage<User> findAll(Role role, UserStatus status, String emailContains, int page, int size) {
        Specification<UserEntity> where = equal("role", role)
                .and(equal("status", status))
                .and(emailContaining(emailContains));
        Page<UserEntity> found = jpa.findAll(where, PageRequest.of(page, size, NEWEST_FIRST));
        return new ResultPage<>(found.map(UserEntity::toDomain).getContent(), page, size, found.getTotalElements());
    }

    @Override
    public User save(User user) {
        return jpa.save(UserEntity.fromDomain(user)).toDomain();
    }

    /**
     * An optional filter: {@code null} means "do not filter". A specification rather than
     * {@code :param is null or ...}, for the reason BookingRepositoryAdapter gives.
     */
    private static Specification<UserEntity> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }

    /** "%" and "_" typed in the search box are characters to look for, not wildcards. */
    private static Specification<UserEntity> emailContaining(String text) {
        if (text == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + escaped + "%", LIKE_ESCAPE);
    }
}
