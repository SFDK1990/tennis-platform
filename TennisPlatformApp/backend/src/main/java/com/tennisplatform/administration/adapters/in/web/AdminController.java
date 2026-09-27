package com.tennisplatform.administration.adapters.in.web;

import com.tennisplatform.administration.adapters.in.web.AdminDtos.ConfigurationResponse;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.RoleFilter;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.StatusFilter;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.TargetStatus;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.UpdateConfigurationRequest;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.UpdateUserStatusRequest;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.UserPageResponse;
import com.tennisplatform.administration.adapters.in.web.AdminDtos.UserResponse;
import com.tennisplatform.administration.application.port.in.AdministerUsers;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.platform.application.port.in.ManagePlatformSettings;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Everything under /admin is for administrators only; the check is the first line of each handler. */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ManagePlatformSettings settings;
    private final AdministerUsers users;

    AdminController(ManagePlatformSettings settings, AdministerUsers users) {
        this.settings = settings;
        this.users = users;
    }

    @GetMapping("/configuration")
    ConfigurationResponse configuration(@AuthenticationPrincipal AuthenticatedUser caller) {
        requireAdmin(caller);
        return ConfigurationResponse.from(settings.current());
    }

    @PatchMapping("/configuration")
    ConfigurationResponse changeConfiguration(@AuthenticationPrincipal AuthenticatedUser caller,
                                              @Valid @RequestBody UpdateConfigurationRequest request) {
        requireAdmin(caller);
        return ConfigurationResponse.from(
                settings.change(caller.id(), request.studentLimit(), request.maxGroupCapacity()));
    }

    @GetMapping("/users")
    UserPageResponse users(@AuthenticationPrincipal AuthenticatedUser caller,
                           @RequestParam(required = false) RoleFilter role,
                           @RequestParam(required = false) StatusFilter status,
                           @RequestParam(required = false) String query,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        requireAdmin(caller);
        return UserPageResponse.from(users.list(
                role == null ? null : role.name(),
                status == null ? null : status.name(),
                query, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
    }

    @PatchMapping("/users/{id}/status")
    UserResponse changeStatus(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
                              @Valid @RequestBody UpdateUserStatusRequest request) {
        requireAdmin(caller);
        return UserResponse.from(users.changeStatus(id, request.status() == TargetStatus.ACTIVE));
    }

    private static void requireAdmin(AuthenticatedUser caller) {
        if (!caller.isAdmin()) {
            throw ForbiddenOperationException.roleNotAllowed("Only an administrator may use the console");
        }
    }
}
