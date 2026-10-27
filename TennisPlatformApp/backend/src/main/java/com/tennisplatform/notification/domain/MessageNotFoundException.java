package com.tennisplatform.notification.domain;

/** No message with that id among the teacher's. Maps to 404. */
public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(String message) {
        super(message);
    }
}
