package com.tennisplatform.lesson.application.service;

import com.tennisplatform.lesson.application.port.in.Courts;
import com.tennisplatform.lesson.application.port.out.CourtRepository;
import com.tennisplatform.lesson.domain.Court;
import com.tennisplatform.lesson.domain.CourtNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class CourtsService implements Courts {

    private static final Logger log = LoggerFactory.getLogger(CourtsService.class);

    private final CourtRepository courts;
    private final TeacherLessons teacher;

    public CourtsService(CourtRepository courts, GetTeacherProfile teacherProfile) {
        this.courts = courts;
        this.teacher = new TeacherLessons(teacherProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtView> all() {
        return courts.findAll().stream().map(CourtsService::view).toList();
    }

    @Override
    @Transactional
    public CourtView rename(UUID teacherUserId, UUID courtId, String name) {
        teacher.requireTheTeacher(teacherUserId);
        Court court = courts.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException("No court with id " + courtId));
        Court saved = courts.save(court.renamed(name));
        log.info("Court {} renamed", courtId);
        return view(saved);
    }

    private static CourtView view(Court court) {
        return new CourtView(court.id(), court.name(), court.position());
    }
}
