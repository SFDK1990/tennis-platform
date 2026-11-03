package com.tennisplatform.notification.application.service;

import com.tennisplatform.notification.application.port.in.NotifyProgress;
import com.tennisplatform.notification.application.port.out.MessageRepository;
import com.tennisplatform.notification.domain.Message;
import com.tennisplatform.notification.domain.MessageAudience;
import com.tennisplatform.student.application.port.in.ObjectiveAchieved;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.LevelView;
import com.tennisplatform.teacher.application.port.in.ObjectiveView;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * The notice on the student's board for an achieved objective (44-fase29-analisis-progreso.md,
 * decision 5). It is a message to one student with its own audience, so the board and the export
 * show it like the others, the teacher's sent messages leave it out, and no email goes with it.
 */
public class ProgressNotifications implements NotifyProgress {

    private static final Logger log = LoggerFactory.getLogger(ProgressNotifications.class);

    private final MessageRepository messages;
    private final QueryLevels levels;
    private final GetTeacherProfile teachers;
    private final Clock clock;

    public ProgressNotifications(MessageRepository messages, QueryLevels levels, GetTeacherProfile teachers,
                                 Clock clock) {
        this.messages = messages;
        this.levels = levels;
        this.teachers = teachers;
        this.clock = clock;
    }

    /**
     * Runs after the mark has committed, in a transaction of its own: in the request's thread the
     * committed one is still bound and could not write. An objective retired since leaves no notice.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void achieved(ObjectiveAchieved achievement) {
        ObjectiveView objective = levels.allObjectivesOf(achievement.teacherUserId()).stream()
                .filter(candidate -> candidate.id().equals(achievement.objectiveId()))
                .findFirst()
                .orElse(null);
        if (objective == null || objective.retired()) {
            return;
        }
        String level = levels.levelsOf(achievement.teacherUserId()).stream()
                .filter(candidate -> candidate.id().equals(objective.levelId()))
                .map(LevelView::name)
                .findFirst()
                .orElse("");
        String teacher = teachers.byUserId(achievement.teacherUserId())
                .map(TeacherProfileView::displayName)
                .orElse("Tu profesor");
        Message notice = new Message(null, achievement.teacherUserId(), MessageAudience.PROGRESS, "Progreso",
                "Objetivo conseguido: " + objective.title(),
                teacher + " ha marcado «" + objective.title() + "», de " + level
                        + ", como conseguido. Lo tienes en «Mi progreso».",
                clock.instant(), 1);
        Message saved = messages.save(notice, List.of(achievement.studentUserId()));
        log.info("Progress notice {} for objective {}", saved.id(), achievement.objectiveId());
    }
}
