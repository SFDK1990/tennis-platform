package com.tennisplatform.pass.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What the teacher does with the passes of their students (decision 3). The app never charges. */
public interface ManagePasses {

    /** Records a pass the student paid for. The student has to be one the teacher manages. */
    PassView record(UUID teacherUserId, UUID studentUserId, UUID passTypeId, LocalDate paidOn, String paymentMethod);

    /** The passes of the teacher's students, or of one of them when {@code studentUserId} is given. */
    List<PassView> ofTeacher(UUID teacherUserId, UUID studentUserId);

    /** Cancels a pass recorded by mistake, only while no lesson has been booked with it. */
    PassView voidPass(UUID teacherUserId, UUID passId);
}
