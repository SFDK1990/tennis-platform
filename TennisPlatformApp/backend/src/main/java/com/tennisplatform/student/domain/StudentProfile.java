package com.tennisplatform.student.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The personal data of a student.
 *
 * <p>Identified by the account it belongs to, like the teacher profile: one profile per user,
 * and a surrogate key would allow a second one.
 *
 * <p>It is born when the student fills their data in, not when they register
 * (16-fase6-analisis-perfiles.md): the sign-up flow is register, verify, then complete the
 * personal data. That is why {@code fullName} can be required here without making it a
 * registration field - and why {@code GET /me} answers with nulls until this exists, which is
 * how the frontend knows it has to ask.
 *
 * <p>{@code nationalId} and {@code address} are restricted personal data
 * (02-arquitectura.md): they may only reach the student themselves, the teacher managing
 * them or an ADMIN, and they never appear in a listing, a search or a log line.
 */
public class StudentProfile {

    /** What a deleted student is called wherever their past bookings still show. */
    public static final String DELETED_STUDENT_NAME = "Alumno eliminado";

    private static final int MAX_FULL_NAME_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 30;
    private static final int MAX_NATIONAL_ID_LENGTH = 30;
    private static final int MAX_ADDRESS_LENGTH = 255;

    private final UUID userId;
    private String fullName;
    private String phone;
    private String nationalId;
    private String address;
    private final Instant createdAt;

    private StudentProfile(UUID userId, String fullName, String phone, String nationalId,
                           String address, Instant createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.nationalId = nationalId;
        this.address = address;
        this.createdAt = createdAt;
    }

    public static StudentProfile create(UUID userId, String fullName, String phone,
                                        String nationalId, String address, Instant now) {
        return new StudentProfile(userId, requiredFullName(fullName),
                optional(phone, MAX_PHONE_LENGTH, "phone"),
                optional(nationalId, MAX_NATIONAL_ID_LENGTH, "national id"),
                optional(address, MAX_ADDRESS_LENGTH, "address"), now);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static StudentProfile rehydrate(UUID userId, String fullName, String phone,
                                           String nationalId, String address, Instant createdAt) {
        return new StudentProfile(userId, fullName, phone, nationalId, address, createdAt);
    }

    /**
     * Applies a partial update: null means "leave as it is", which is what lets PATCH change
     * the phone without resending everything. An empty string clears an optional field, since
     * a nullable value needs a way of being emptied on purpose - but it cannot clear the name,
     * which is required and would leave the row unusable.
     */
    public void update(String newFullName, String newPhone, String newNationalId,
                       String newAddress) {
        if (newFullName != null) {
            this.fullName = requiredFullName(newFullName);
        }
        if (newPhone != null) {
            this.phone = optional(newPhone, MAX_PHONE_LENGTH, "phone");
        }
        if (newNationalId != null) {
            this.nationalId = optional(newNationalId, MAX_NATIONAL_ID_LENGTH, "national id");
        }
        if (newAddress != null) {
            this.address = optional(newAddress, MAX_ADDRESS_LENGTH, "address");
        }
    }

    /**
     * The student deleted their account: the profile stays, so their past bookings still have a
     * name to show, but nothing in it identifies them any more (01-analisis-funcional.md §18).
     */
    public void anonymize() {
        this.fullName = DELETED_STUDENT_NAME;
        this.phone = null;
        this.nationalId = null;
        this.address = null;
    }

    private static String requiredFullName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidStudentProfileException("The full name is required");
        }
        if (trimmed.length() > MAX_FULL_NAME_LENGTH) {
            throw new InvalidStudentProfileException(
                    "The full name cannot exceed " + MAX_FULL_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    /** The field name travels into the message; the value never does, because it is personal. */
    private static String optional(String value, int maxLength, String field) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new InvalidStudentProfileException(
                    "The " + field + " cannot exceed " + maxLength + " characters");
        }
        return trimmed;
    }

    public UUID userId() {
        return userId;
    }

    public String fullName() {
        return fullName;
    }

    public String phone() {
        return phone;
    }

    public String nationalId() {
        return nationalId;
    }

    public String address() {
        return address;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
