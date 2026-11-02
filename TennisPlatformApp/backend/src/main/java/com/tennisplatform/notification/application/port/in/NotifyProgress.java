package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.student.application.port.in.ObjectiveAchieved;

/**
 * Leaves a notice on the student's board when the teacher marks an objective as achieved (Fase 29,
 * decision 5): no email. Called after the commit.
 */
public interface NotifyProgress {

    void achieved(ObjectiveAchieved achievement);
}
