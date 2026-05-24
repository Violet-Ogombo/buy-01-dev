package com.example.mediaservice.exception;

public class MediaAccessDeniedException extends RuntimeException {
    public MediaAccessDeniedException(String message) {
        super(message);
    }
}
