package com.example.order.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Order not found");
        ResponseEntity<Map<String, String>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Order not found");
    }

    @Test
    void handleResourceNotFound_nullMessage_returnsEmptyString() {
        ResourceNotFoundException ex = new ResourceNotFoundException(null);
        ResponseEntity<Map<String, String>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "");
    }

    @Test
    void handleAccessDenied_returns403WithMessage() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Forbidden");
    }

    @Test
    void handleAccessDenied_nullMessage_returnsEmptyString() {
        AccessDeniedException ex = new AccessDeniedException(null);
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "");
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad input");
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Bad input");
        assertThat(response.getBody()).containsEntry("error", "Bad input");
    }

    @Test
    void handleIllegalArgument_nullMessage_returnsEmptyString() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "");
    }

    @Test
    void handleValidationErrors_returnsBadRequestWithFieldErrors() throws Exception {
        // Use MapBindingResult — avoids requiring a bean property definition
        org.springframework.validation.MapBindingResult bindingResult =
                new org.springframework.validation.MapBindingResult(new java.util.HashMap<>(), "target");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "target", "email", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("email", "must not be blank");
    }

    @Test
    void resourceNotFoundException_messageIsPreserved() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test message");
        assertThat(ex.getMessage()).isEqualTo("test message");
    }

    @Test
    void accessDeniedException_messageIsPreserved() {
        AccessDeniedException ex = new AccessDeniedException("no access");
        assertThat(ex.getMessage()).isEqualTo("no access");
    }
}
