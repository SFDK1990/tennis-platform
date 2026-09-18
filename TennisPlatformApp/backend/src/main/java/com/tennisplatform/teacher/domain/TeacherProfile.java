package com.tennisplatform.teacher.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.UUID;

/**
 * The operational profile of the single teacher of the MVP.
 *
 * <p>Identified by the account it belongs to: there is no surrogate id, because a user has at
 * most one teacher profile and a separate key would allow a second row per user.
 *
 * <p>The time zone is a {@link ZoneId} rather than a string on purpose. It is the reference for
 * every lesson shown in local time and for the "does not cross midnight" rule, so an invalid or
 * abandoned zone id has to be rejected the moment it enters the domain, not the day a lesson is
 * scheduled.
 */
public class TeacherProfile {

    private static final int MAX_DISPLAY_NAME_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 30;

    private final UUID userId;
    private String displayName;
    private String phone;
    private ZoneId timezone;
    private final Instant createdAt;

    private TeacherProfile(UUID userId, String displayName, String phone, ZoneId timezone,
                           Instant createdAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.phone = phone;
        this.timezone = timezone;
        this.createdAt = createdAt;
    }

    public static TeacherProfile create(UUID userId, String displayName, String phone,
                                        String timezone, Instant now) {
        return new TeacherProfile(userId, validDisplayName(displayName), validPhone(phone),
                validTimezone(timezone), now);
    }

    /** Rehydration from persistence. Stored values are not re-validated. */
    public static TeacherProfile rehydrate(UUID userId, String displayName, String phone,
                                           ZoneId timezone, Instant createdAt) {
        return new TeacherProfile(userId, displayName, phone, timezone, createdAt);
    }

    /**
     * Applies a partial update: a null argument means "leave as it is", which is what allows
     * PATCH to change the phone without resending the name. Clearing the phone is expressed as
     * an empty string, since a nullable field needs a way to be emptied on purpose.
     */
    public void update(String newDisplayName, String newPhone, String newTimezone) {
        if (newDisplayName != null) {
            this.displayName = validDisplayName(newDisplayName);
        }
        if (newPhone != null) {
            this.phone = validPhone(newPhone);
        }
        if (newTimezone != null) {
            this.timezone = validTimezone(newTimezone);
        }
    }

    private static String validDisplayName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidTeacherProfileException("The display name is required");
        }
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new InvalidTeacherProfileException(
                    "The display name cannot exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String validPhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_PHONE_LENGTH) {
            throw new InvalidTeacherProfileException(
                    "The phone cannot exceed " + MAX_PHONE_LENGTH + " characters");
        }
        return trimmed;
    }

    private static ZoneId validTimezone(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidTeacherProfileException("The time zone is required");
        }
        try {
            return ZoneId.of(trimmed);
        } catch (ZoneRulesException e) {
            throw new InvalidTeacherProfileException("Unknown time zone: " + trimmed);
        } catch (RuntimeException e) {
            throw new InvalidTeacherProfileException("Not a valid IANA time zone: " + trimmed);
        }
    }

    public UUID userId() {
        return userId;
    }

    public String displayName() {
        return displayName;
    }

    public String phone() {
        return phone;
    }

    public ZoneId timezone() {
        return timezone;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
