package com.broCode.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Username is already taken");
    }
}
