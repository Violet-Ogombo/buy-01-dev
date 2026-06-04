package com.example.mediaservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageFileNotFoundExceptionTest {

    @Test
    void constructor_createsExceptionWithMessage() {
        String message = "Image file not found";
        ImageFileNotFoundException exception = new ImageFileNotFoundException(message);

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void constructor_preservesExceptionMessage() {
        String message = "File does not exist at path /uploads/image123.jpg";
        ImageFileNotFoundException exception = new ImageFileNotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void exceptionCanBeCaught() {
        try {
            throw new ImageFileNotFoundException("Test exception");
        } catch (ImageFileNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("Test exception");
        }
    }
}
