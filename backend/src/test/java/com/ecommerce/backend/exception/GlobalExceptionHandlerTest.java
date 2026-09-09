package com.ecommerce.backend.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    private final MethodParameter methodParameter;

    {
        try {
            methodParameter = new MethodParameter(
                    GlobalExceptionHandler.class.getMethod("handleIllegalArgumentException",
                            IllegalArgumentException.class,
                            org.springframework.web.context.request.WebRequest.class),
                    0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private WebRequest createWebRequest() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/test");
        return request;
    }

    @Test
    void handleIllegalArgumentException_returns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Bad input"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("Bad input");
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        assertThat(response.getBody().get("status")).isEqualTo(400);
    }

    @Test
    void handleIllegalStateException_returns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalStateException(new IllegalStateException("Conflict"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).isEqualTo("Conflict");
        assertThat(response.getBody().get("error")).isEqualTo("Conflict");
        assertThat(response.getBody().get("status")).isEqualTo(409);
    }

    @Test
    void handleValidationErrors_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("object", "name", "Name cannot be empty");
        FieldError fieldError2 = new FieldError("object", "email", "Email is invalid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);
        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationErrors(ex, createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("Bad Request");
        String message = (String) response.getBody().get("message");
        assertThat(message).contains("name: Name cannot be empty");
        assertThat(message).contains("email: Email is invalid");
    }

    @Test
    void handleValidationErrors_emptyErrors_returnsExceptionMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);
        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationErrors(ex, createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleUsernameNotFoundException_returns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUsernameNotFoundException(new UsernameNotFoundException("User not found"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message")).isEqualTo("User not found");
        assertThat(response.getBody().get("error")).isEqualTo("Not Found");
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    @Test
    void handleGeneralException_returns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneralException(new RuntimeException("Unexpected error"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
        assertThat(response.getBody().get("status")).isEqualTo(500);
    }

    @Test
    void handleUserNotFoundException_returns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUserNotFoundException(new com.ecommerce.backend.exception.UserNotFoundException("No account found with email: ghost@example.com"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message")).isEqualTo("No account found with email: ghost@example.com");
        assertThat(response.getBody().get("error")).isEqualTo("Not Found");
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    @Test
    void handleAuthenticationException_returns401WithMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAuthenticationException(new org.springframework.security.authentication.BadCredentialsException("Invalid password for email: john@example.com"), createWebRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("message")).isEqualTo("Invalid password for email: john@example.com");
        assertThat(response.getBody().get("error")).isEqualTo("Unauthorized");
        assertThat(response.getBody().get("status")).isEqualTo(401);
    }

    @Test
    void allHandlers_includeTimestampAndPath() {
        WebRequest request = createWebRequest();

        ResponseEntity<Map<String, Object>> r1 = handler.handleIllegalArgumentException(new IllegalArgumentException("e"), request);
        assertThat(r1.getBody().get("timestamp")).isNotNull();
        assertThat(r1.getBody().get("path")).isEqualTo("uri=/api/test");

        ResponseEntity<Map<String, Object>> r2 = handler.handleIllegalStateException(new IllegalStateException("e"), request);
        assertThat(r2.getBody().get("timestamp")).isNotNull();
        assertThat(r2.getBody().get("path")).isNotNull();

        ResponseEntity<Map<String, Object>> r3 = handler.handleUsernameNotFoundException(new UsernameNotFoundException("e"), request);
        assertThat(r3.getBody().get("timestamp")).isNotNull();

        ResponseEntity<Map<String, Object>> r4 = handler.handleGeneralException(new Exception("e"), request);
        assertThat(r4.getBody().get("timestamp")).isNotNull();
    }
}
