package com.tennisplatform.student.domain;

import java.util.Arrays;
import java.util.UUID;

/**
 * A student's photo (39-fase25.5-analisis-foto-alumno.md). Its type is read from its first bytes,
 * never from what the client says it is: a file called {@code .jpg} that is not an image is refused.
 */
public final class StudentPhoto {

    /** The browser sends it at 400 x 400, some 40 KB; this is the margin for one that does not. */
    public static final int MAX_BYTES = 1024 * 1024;

    private final UUID studentUserId;
    private final String contentType;
    private final byte[] bytes;

    private StudentPhoto(UUID studentUserId, String contentType, byte[] bytes) {
        this.studentUserId = studentUserId;
        this.contentType = contentType;
        this.bytes = bytes.clone();
    }

    public static StudentPhoto of(UUID studentUserId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidPhotoException("The photo is empty");
        }
        if (bytes.length > MAX_BYTES) {
            throw new InvalidPhotoException("A photo has at most " + MAX_BYTES + " bytes");
        }
        return new StudentPhoto(studentUserId, typeOf(bytes), bytes);
    }

    public static StudentPhoto rehydrate(UUID studentUserId, String contentType, byte[] bytes) {
        return new StudentPhoto(studentUserId, contentType, bytes);
    }

    private static String typeOf(byte[] bytes) {
        if (startsWith(bytes, 0, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(bytes, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (startsWith(bytes, 0, 'R', 'I', 'F', 'F') && startsWith(bytes, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        throw new InvalidPhotoException("A photo is a JPEG, PNG or WebP image");
    }

    private static boolean startsWith(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    public UUID studentUserId() {
        return studentUserId;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StudentPhoto photo && studentUserId.equals(photo.studentUserId)
                && contentType.equals(photo.contentType) && Arrays.equals(bytes, photo.bytes);
    }

    @Override
    public int hashCode() {
        return studentUserId.hashCode();
    }
}
