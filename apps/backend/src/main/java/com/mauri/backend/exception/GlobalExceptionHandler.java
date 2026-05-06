package com.mauri.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({
            CsvImportValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(CsvImportProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleCsvImportProcessingException(
            CsvImportProcessingException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter value.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        ResponseEntity<ApiErrorResponse> nestedResponse = resolveNestedKnownException(exception, request);
        if (nestedResponse != null) {
            return nestedResponse;
        }

        log.error("Unhandled exception while processing API request.", exception);

        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ApiErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(path);
        return response;
    }

    private ResponseEntity<ApiErrorResponse> resolveNestedKnownException(Exception exception, HttpServletRequest request) {
        Throwable notFoundCause = findCause(exception, ResourceNotFoundException.class);
        if (notFoundCause != null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    notFoundCause.getMessage(),
                    request.getRequestURI()
            ));
        }

        Throwable badRequestCause = firstNonNull(
                findCause(exception, CsvImportValidationException.class),
                findCause(exception, ConstraintViolationException.class),
                findCause(exception, MethodArgumentNotValidException.class),
                findCause(exception, IllegalArgumentException.class)
        );
        if (badRequestCause != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    badRequestCause.getMessage(),
                    request.getRequestURI()
            ));
        }

        Throwable processingCause = findCause(exception, CsvImportProcessingException.class);
        if (processingCause != null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(buildErrorResponse(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    processingCause.getMessage(),
                    request.getRequestURI()
            ));
        }

        return null;
    }

    private Throwable firstNonNull(Throwable... causes) {
        for (Throwable cause : causes) {
            if (cause != null) {
                return cause;
            }
        }
        return null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }
}
