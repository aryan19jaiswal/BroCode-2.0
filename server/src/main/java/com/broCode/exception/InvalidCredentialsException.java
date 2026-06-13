package com.broCode.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Incorrect email/username or password");
    }
}
