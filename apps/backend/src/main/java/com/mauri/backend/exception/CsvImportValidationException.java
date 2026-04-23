package com.mauri.backend.exception;

public class CsvImportValidationException extends RuntimeException {

    public CsvImportValidationException(String message) {
        super(message);
    }

    public CsvImportValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
