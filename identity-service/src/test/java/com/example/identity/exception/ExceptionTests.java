package com.example.identity.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateEmailExceptionTest {

    @Test
    void duplicateEmailException_createsWithMessage() {
        // Given
        String message = "Email already exists";
        
        // When
        DuplicateEmailException exception = new DuplicateEmailException(message);
        
        // Then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message);
    }
}

class InvalidPasswordExceptionTest {

    @Test
    void invalidPasswordException_createsWithMessage() {
        // Given
        String message = "Invalid password";
        
        // When
        InvalidPasswordException exception = new InvalidPasswordException(message);
        
        // Then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message);
    }
}
