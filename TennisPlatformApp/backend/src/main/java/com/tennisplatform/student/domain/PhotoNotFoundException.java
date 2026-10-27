package com.tennisplatform.student.domain;

/** No photo, or one the caller may not see: both answer the same. Maps to 404 PHOTO_NOT_FOUND. */
public class PhotoNotFoundException extends RuntimeException {

    public PhotoNotFoundException(String message) {
        super(message);
    }
}
