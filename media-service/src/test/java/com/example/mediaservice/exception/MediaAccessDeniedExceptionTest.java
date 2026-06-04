package com.example.mediaservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaAccessDeniedExceptionTest {

    @Test
    void constructor_createsExceptionWithMessage() {
        String message = "Unauthorized to delete this media";
        MediaAccessDeniedException exception = new MediaAccessDeniedException(message);

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void constructor_preservesExceptionMessage() {
        String message = "User 123 does not own media 456";
        MediaAccessDeniedException exception = new MediaAccessDeniedException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    void exceptionCanBeCaught() {
        try {
            throw new MediaAccessDeniedException("Access denied");
        } catch (MediaAccessDeniedException e) {
            assertThat(e.getMessage()).isEqualTo("Access denied");
        }
    }
}
