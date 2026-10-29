package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.lesson.application.port.in.LessonCourtChanged;

/**
 * Tells the students with a seat that their lesson moved to another court (Fase 25.6). Called
 * after the commit; like {@link NotifyBookings}, it never throws.
 */
public interface NotifyCourtChanges {

    void changed(LessonCourtChanged change);
}
