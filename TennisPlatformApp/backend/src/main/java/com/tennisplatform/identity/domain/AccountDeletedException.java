package com.tennisplatform.identity.domain;

/** A deleted account is final: it can be neither disabled nor reactivated. */
public class AccountDeletedException extends RuntimeException {

    public AccountDeletedException() {
        super("The account was deleted");
    }
}
