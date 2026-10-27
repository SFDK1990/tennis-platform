package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.shared.domain.ForbiddenOperationException;
import com.tennisplatform.student.application.port.in.StudentPhotos;
import com.tennisplatform.student.application.port.in.StudentPhotos.PhotoView;
import com.tennisplatform.student.domain.InvalidPhotoException;
import com.tennisplatform.student.domain.PhotoNotFoundException;
import com.tennisplatform.student.domain.StudentPhoto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * The student's photo (Fase 25.5). The image travels as itself, not inside JSON; the frontend asks
 * for it with its token, because an {@code <img>} pointing here could not carry one.
 */
@RestController
class StudentPhotoController {

    private final StudentPhotos photos;

    StudentPhotoController(StudentPhotos photos) {
        this.photos = photos;
    }

    /** Read up to one byte past the limit: a huge body is never held whole to be refused. */
    @PutMapping("/api/v1/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void change(@AuthenticationPrincipal AuthenticatedUser caller, HttpServletRequest request) throws IOException {
        requireStudent(caller);
        byte[] image = request.getInputStream().readNBytes(StudentPhoto.MAX_BYTES + 1);
        if (image.length > StudentPhoto.MAX_BYTES) {
            throw new InvalidPhotoException("A photo has at most " + StudentPhoto.MAX_BYTES + " bytes");
        }
        photos.change(caller.id(), image);
    }

    @DeleteMapping("/api/v1/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal AuthenticatedUser caller) {
        requireStudent(caller);
        photos.remove(caller.id());
    }

    @GetMapping("/api/v1/me/photo")
    ResponseEntity<byte[]> mine(@AuthenticationPrincipal AuthenticatedUser caller) {
        return image(photos.ofStudent(caller.id()));
    }

    @GetMapping("/api/v1/teacher/students/{userId}/photo")
    ResponseEntity<byte[]> ofStudent(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID userId) {
        if (!caller.isTeacher()) {
            throw ForbiddenOperationException.teacherOnly("Only the teacher sees their students' photos");
        }
        return image(photos.ofManagedStudent(caller.id(), userId));
    }

    /** Personal data: never kept by a shared cache. */
    private static ResponseEntity<byte[]> image(Optional<PhotoView> photo) {
        PhotoView found = photo.orElseThrow(() -> new PhotoNotFoundException("There is no photo"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(found.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(found.bytes());
    }

    private static void requireStudent(AuthenticatedUser caller) {
        if (!caller.isStudent()) {
            throw ForbiddenOperationException.roleNotAllowed("Only a student has a photo");
        }
    }
}
