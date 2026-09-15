package com.tennisplatform.identity.application.port.out;

import com.tennisplatform.identity.domain.EmailAddress;

public interface IdentityMailer {

    void sendEmailVerification(EmailAddress recipient, String rawToken);

    void sendPasswordReset(EmailAddress recipient, String rawToken);

    /**
     * Sent to an existing address when someone tries to register with it again. The
     * registration response itself stays identical either way, so this notifies the real
     * owner without telling the requester whether the account existed.
     */
    void sendRegistrationAttemptOnExistingAccount(EmailAddress recipient);
}
