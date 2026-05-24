package com.example.mediaservice.exception;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(String message) {
        super(message);
    }
}
