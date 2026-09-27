package com.tennisplatform.identity;

import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.domain.EmailAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for SMTP during tests and keeps the tokens that would have been emailed, which is
 * the only way a test can follow a verification or reset link: the raw token exists nowhere
 * else, since the database only ever stores its hash.
 */
public class RecordingMailer implements IdentityMailer {

    private final List<String> verificationTokens = new ArrayList<>();
    private final List<String> resetTokens = new ArrayList<>();
    private final List<EmailAddress> existingAccountWarnings = new ArrayList<>();

    @Override
    public void sendEmailVerification(EmailAddress recipient, String rawToken) {
        verificationTokens.add(rawToken);
    }

    @Override
    public void sendPasswordReset(EmailAddress recipient, String rawToken) {
        resetTokens.add(rawToken);
    }

    @Override
    public void sendRegistrationAttemptOnExistingAccount(EmailAddress recipient) {
        existingAccountWarnings.add(recipient);
    }

    public String lastVerificationToken() {
        return verificationTokens.get(verificationTokens.size() - 1);
    }

    public String lastResetToken() {
        return resetTokens.get(resetTokens.size() - 1);
    }

    public int verificationCount() {
        return verificationTokens.size();
    }

    public int resetCount() {
        return resetTokens.size();
    }

    public int existingAccountWarningCount() {
        return existingAccountWarnings.size();
    }

    public void clear() {
        verificationTokens.clear();
        resetTokens.clear();
        existingAccountWarnings.clear();
    }
}
