package com.example.product.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MediaValidationExceptionTest {

    @Test
    void messageConstructor_preservesMessage() {
        MediaValidationException ex = new MediaValidationException("invalid media");
        assertThat(ex).hasMessage("invalid media");
    }

    @Test
    void messageAndCause_constructorChainsCause() {
        RuntimeException cause = new RuntimeException("root");
        MediaValidationException ex = new MediaValidationException("failed", cause);
        assertThat(ex).hasMessage("failed");
        assertThat(ex).hasCause(cause);
    }

    @Test
    void isRuntimeException() {
        try {
            throw new MediaValidationException("boom");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(MediaValidationException.class);
        }
    }
}
