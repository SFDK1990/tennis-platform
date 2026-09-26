package com.tennisplatform.lesson.domain;

/**
 * The caller is not the teacher who owns the schedule being changed.
 *
 * <p>Duplicated on purpose, for the third time. {@code student} and {@code availability} each
 * have their own, and {@code teacher} has {@code NotTheTeacherException}; all of them map to
 * {@code TEACHER_FORBIDDEN}. The module boundary rules forbid depending on anything of another
 * module except its {@code application/port/in}, and a domain exception is not that. Sharing it
 * would mean putting business meaning in {@code shared}, which exists to hold none.
 */
public class TeacherRoleRequiredException extends RuntimeException {

    public TeacherRoleRequiredException(String message) {
        super(message);
    }
}
