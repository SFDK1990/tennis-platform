package com.tennisplatform.teacher.application.port.in;

/** Fields a teacher may change about themselves. Null means "not submitted". */
public record TeacherProfileUpdate(String displayName, String phone, String timezone) {
}
