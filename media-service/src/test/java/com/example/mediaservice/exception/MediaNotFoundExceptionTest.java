package com.example.mediaservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaNotFoundExceptionTest {

    @Test
    void constructor_createsExceptionWithMessage() {
        String message = "Media not found";
        MediaNotFoundException exception = new MediaNotFoundException(message);

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void constructor_preservesExceptionMessage() {
        String message = "Media with id abc123 does not exist";
        MediaNotFoundException exception = new MediaNotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void exceptionCanBeCaught() {
        try {
            throw new MediaNotFoundException("Not found");
        } catch (MediaNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo("Not found");
        }
    }
}
