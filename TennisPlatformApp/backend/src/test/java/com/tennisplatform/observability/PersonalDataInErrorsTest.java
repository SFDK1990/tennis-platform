package com.tennisplatform.observability;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.service.ProvisionAdminAccountService;
import com.tennisplatform.identity.domain.EmailAddress;
import com.tennisplatform.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * An unhandled exception reaches the log whole - message and every cause - and a startup failure
 * printed the teacher's address that way. Each test renders the trace as Logback would and looks
 * for the address in it. The bootstraps add nothing to what these services throw, so the
 * services are called directly.
 */
class PersonalDataInErrorsTest extends AbstractIntegrationTest {

    private static final String EMAIL = "someone-personal@example.com";

    @Autowired
    private UserRepository users;

    @Autowired
    private ProvisionTeacherAccount teacherAccounts;

    @Autowired
    private ProvisionAdminAccountService adminAccounts;

    /**
     * Holds only while the driver leaves out the server's detail (logServerErrorDetail in
     * application.yml): without it the trace quotes the duplicate key.
     */
    @Test
    void aConstraintViolationDoesNotQuoteTheRow() {
        users.save(aStudent());

        Throwable thrown = catchThrowable(() -> users.save(aStudent()));

        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(printed(thrown)).contains("ux_users_email").doesNotContain(EMAIL);
    }

    @Test
    void theTeacherBootstrapNamesTheVariableNotTheAddressWhenItIsTaken() {
        users.save(aStudent());

        Throwable thrown = catchThrowable(() -> teacherAccounts.provision(EMAIL, "a-valid-password"));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TEACHER_EMAIL");
        assertThat(printed(thrown)).doesNotContain(EMAIL);
    }

    @Test
    void theAdminBootstrapRejectsAWeakPasswordWithoutTheAddress() {
        Throwable thrown = catchThrowable(() -> adminAccounts.provision(EMAIL, "short"));

        assertThat(thrown).isNotNull();
        assertThat(printed(thrown)).doesNotContain(EMAIL);
    }

    private static User aStudent() {
        return User.register(new EmailAddress(EMAIL), "not-a-real-hash", Instant.now());
    }

    private static String printed(Throwable thrown) {
        StringWriter trace = new StringWriter();
        thrown.printStackTrace(new PrintWriter(trace));
        return trace.toString();
    }
}
