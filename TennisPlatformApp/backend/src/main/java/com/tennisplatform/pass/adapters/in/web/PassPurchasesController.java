package com.tennisplatform.pass.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.pass.application.port.in.BuyPasses;
import com.tennisplatform.pass.application.port.in.PurchaseView;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A student buys a pass in the app (Fase 27.5). The answer carries the gateway's page, where the
 * frontend sends them; the pass appears when the gateway confirms the payment.
 */
@RestController
@RequestMapping("/api/v1")
class PassPurchasesController {

    private final BuyPasses buyPasses;

    PassPurchasesController(BuyPasses buyPasses) {
        this.buyPasses = buyPasses;
    }

    @PostMapping("/me/pass-purchases")
    @ResponseStatus(HttpStatus.CREATED)
    PurchaseView start(@AuthenticationPrincipal AuthenticatedUser caller,
                       @Valid @RequestBody StartPurchaseRequest request) {
        requireStudent(caller);
        return buyPasses.start(caller.id(), caller.emailVerified(), request.passTypeId());
    }

    @GetMapping("/me/pass-purchases/{id}")
    PurchaseView one(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id) {
        requireStudent(caller);
        return buyPasses.ofStudent(caller.id(), id);
    }

    private static void requireStudent(AuthenticatedUser caller) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student buys passes");
        }
    }

    record StartPurchaseRequest(@NotNull UUID passTypeId) {
    }
}
