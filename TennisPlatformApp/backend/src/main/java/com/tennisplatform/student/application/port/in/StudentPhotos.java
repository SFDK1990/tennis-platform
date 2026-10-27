package com.tennisplatform.student.application.port.in;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The student's photo (39-fase25.5-analisis-foto-alumno.md): the student changes it, and only they
 * and the teacher who manages them see it.
 */
public interface StudentPhotos {

    /** Replaces the photo, if any. A file that is not a JPEG, PNG or WebP image is refused. */
    void change(UUID studentUserId, byte[] image);

    void remove(UUID studentUserId);

    Optional<PhotoView> ofStudent(UUID studentUserId);

    /** Empty for a student this teacher does not manage now, as for one without a photo. */
    Optional<PhotoView> ofManagedStudent(UUID teacherUserId, UUID studentUserId);

    /** Which of these students have a photo, so a list does not ask for the ones that do not exist. */
    Set<UUID> withPhoto(Collection<UUID> studentUserIds);

    /** The image and its type. The bytes are copied in and out: an array is never shared. */
    final class PhotoView {

        private final String contentType;
        private final byte[] bytes;

        public PhotoView(String contentType, byte[] bytes) {
            this.contentType = contentType;
            this.bytes = bytes.clone();
        }

        public String contentType() {
            return contentType;
        }

        public byte[] bytes() {
            return bytes.clone();
        }
    }
}
