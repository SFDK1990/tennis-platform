package com.tennisplatform.student.application.service;

import com.tennisplatform.student.application.port.in.GetManagedStudents;
import com.tennisplatform.student.application.port.in.ManagedStudentDetailView;
import com.tennisplatform.student.application.port.in.UpdateStudentRecord;
import com.tennisplatform.student.application.port.out.ManagedStudentRepository;
import com.tennisplatform.student.domain.InvalidStudentProfileException;
import com.tennisplatform.student.domain.ManagedStudent;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class StudentRecordService implements UpdateStudentRecord {

    private static final Logger log = LoggerFactory.getLogger(StudentRecordService.class);

    private final ManagedStudentRepository relationships;
    private final QueryLevels levels;
    private final GetManagedStudents students;

    public StudentRecordService(ManagedStudentRepository relationships, QueryLevels levels,
                                GetManagedStudents students) {
        this.relationships = relationships;
        this.levels = levels;
        this.students = students;
    }

    @Override
    @Transactional
    public ManagedStudentDetailView update(UUID teacherUserId, UUID studentUserId, UUID levelId, String goals,
                                           String privateNotes) {
        ManagedStudent relationship = relationships.findByPair(teacherUserId, studentUserId)
                .filter(ManagedStudent::isManaged)
                .orElseThrow(() -> new StudentNotManagedException("This student is not managed by this teacher"));
        if (levelId != null && !levels.belongsTo(levelId, teacherUserId)) {
            throw new InvalidStudentProfileException("The level is not one of the teacher's");
        }
        relationship.updateRecord(levelId, goals, privateNotes);
        relationships.save(relationship);
        // The id only: what the teacher wrote is personal data and stays out of the log.
        log.info("Record of student {} updated", studentUserId);
        return students.detail(teacherUserId, studentUserId);
    }
}
