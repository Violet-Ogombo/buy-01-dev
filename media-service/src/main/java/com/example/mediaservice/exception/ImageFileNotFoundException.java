package com.example.mediaservice.exception;

public class ImageFileNotFoundException extends RuntimeException {
    public ImageFileNotFoundException(String message) {
        super(message);
    }
}
