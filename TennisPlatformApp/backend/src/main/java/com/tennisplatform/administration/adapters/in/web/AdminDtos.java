package com.tennisplatform.administration.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AccountView;
import com.tennisplatform.platform.application.port.in.PlatformSettingsView;
import com.tennisplatform.shared.domain.ResultPage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class AdminDtos {

    private AdminDtos() {
    }

    /** The filters the list accepts. Enums here so that an unknown value is a 400, not a 500. */
    enum RoleFilter { ADMIN, TEACHER, STUDENT }

    enum StatusFilter { PENDING_VERIFICATION, ACTIVE, DISABLED }

    /** What an administrator may set a status to: pending verification is not a choice. */
    enum TargetStatus { ACTIVE, DISABLED }

    record ConfigurationResponse(int studentLimit, int maxGroupCapacity, Instant updatedAt, UUID updatedBy) {

        static ConfigurationResponse from(PlatformSettingsView view) {
            return new ConfigurationResponse(view.studentLimit(), view.maxGroupCapacity(), view.updatedAt(),
                    view.updatedBy());
        }
    }

    /** Both optional: a field left out keeps its value. */
    record UpdateConfigurationRequest(@Positive Integer studentLimit, @Positive Integer maxGroupCapacity) {
    }

    record UserResponse(UUID id, String email, String role, String status, Instant createdAt) {

        static UserResponse from(AccountView account) {
            return new UserResponse(account.id(), account.email(), account.role(), account.status(),
                    account.createdAt());
        }
    }

    record UserPageResponse(List<UserResponse> items, int page, int size, long totalItems) {

        static UserPageResponse from(ResultPage<AccountView> page) {
            return new UserPageResponse(page.items().stream().map(UserResponse::from).toList(),
                    page.page(), page.size(), page.totalItems());
        }
    }

    record UpdateUserStatusRequest(@NotNull TargetStatus status) {
    }
}
