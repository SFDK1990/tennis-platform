package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.membership.application.port.in.MembershipEnding;
import com.tennisplatform.membership.application.port.in.MembershipStarted;

/**
 * Tells the student and the teacher that somebody joined or left a fixed group (Fase 28): one
 * email for the whole group, not one per lesson. Called after the commit; it never throws.
 */
public interface NotifyMemberships {

    void started(MembershipStarted membership);

    void ending(MembershipEnding membership);
}
