package com.tennisplatform.pass.domain;

/** No purchase with that id, or it is somebody else's. Maps to 404. */
public class PurchaseNotFoundException extends RuntimeException {

    public PurchaseNotFoundException(String message) {
        super(message);
    }
}
