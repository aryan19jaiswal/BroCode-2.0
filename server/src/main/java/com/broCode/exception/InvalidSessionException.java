package com.broCode.exception;

public class InvalidSessionException extends RuntimeException{
    public InvalidSessionException(String sessionId){
        super("Invalid session ID: " + sessionId);
    }
}
