package com.tennisplatform.student.application.port.in;

/**
 * Fields a student may change about themselves. Null means "not submitted"; an empty string
 * clears an optional field.
 *
 * <p>The role, the email and the account status are deliberately absent. They are not "fields
 * we forgot": they belong to the account, and a profile update that could touch them would be
 * a privilege escalation waiting for a client that sends more than it should.
 */
public record StudentProfileUpdate(String fullName, String phone, String nationalId,
                                   String address) {
}
