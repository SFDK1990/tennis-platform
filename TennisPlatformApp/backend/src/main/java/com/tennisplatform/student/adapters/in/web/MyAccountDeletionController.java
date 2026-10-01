package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.DeleteMyAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /me/deletion}: a student deletes their own account. A {@code POST} with the
 * password in the body rather than {@code DELETE /me}, because it carries a body and because
 * the account is not removed - it is emptied (30-fase19-analisis-cierre-mvp.md).
 *
 * <p>Answers 204 and leaves the refresh cookie alone: every session was revoked, and the next
 * refresh clears it like any dead one.
 */
@RestController
class MyAccountDeletionController {

    private final DeleteMyAccount deleteMyAccount;

    MyAccountDeletionController(DeleteMyAccount deleteMyAccount) {
        this.deleteMyAccount = deleteMyAccount;
    }

    @PostMapping("/api/v1/me/deletion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody DeletionRequest request) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed(
                    "The teacher and the administrator are created by the bootstrap and cannot delete themselves");
        }
        deleteMyAccount.delete(caller.id(), request.password());
    }

    record DeletionRequest(@NotBlank String password) {
    }
}
