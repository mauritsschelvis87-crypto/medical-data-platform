package com.mauri.backend.exception;

public class CsvImportProcessingException extends RuntimeException {

    public CsvImportProcessingException(String message) {
        super(message);
    }

    public CsvImportProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
