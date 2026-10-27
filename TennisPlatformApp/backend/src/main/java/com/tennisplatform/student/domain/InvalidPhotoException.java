package com.tennisplatform.student.domain;

/** Not a JPEG, PNG or WebP image, empty, or too large. Maps to 400 PHOTO_INVALID. */
public class InvalidPhotoException extends RuntimeException {

    public InvalidPhotoException(String message) {
        super(message);
    }
}
