package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.adapters.in.web.AuthDtos.UserSummaryResponse;
import com.tennisplatform.identity.adapters.out.security.AuthenticatedUser;
import com.tennisplatform.identity.application.port.in.GetCurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private final GetCurrentUser getCurrentUser;

    MeController(GetCurrentUser getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    /**
     * The caller's id comes from the verified token, never from a parameter - which is what
     * makes it impossible to ask for somebody else's profile by changing a URL.
     */
    @GetMapping
    UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedUser caller) {
        return UserSummaryResponse.from(getCurrentUser.byId(caller.id()));
    }
}
