package com.mauri.backend.exception;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsBadRequestForWrappedIllegalArgumentException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/patients/test/vitals");
        Exception exception = new ServletException(
                "Request processing failed",
                new IllegalArgumentException("Weight is outside a broad plausible range for ADULT; verify unit and source data.")
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "Weight is outside a broad plausible range for ADULT; verify unit and source data.",
                response.getBody().getMessage()
        );
    }

    @Test
    void returnsNotFoundForWrappedResourceNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/patients/missing/vitals");
        Exception exception = new ServletException(
                "Request processing failed",
                new ResourceNotFoundException("Patient not found with id: missing")
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(exception, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Patient not found with id: missing", response.getBody().getMessage());
    }

    @Test
    void keepsGenericInternalServerErrorForUnexpectedExceptions() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/patients/test/vitals");

        ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(
                new RuntimeException("boom"),
                request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
    }
}
