package com.clinic.recp.common.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Request validation failed");

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                detail,
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Request validation failed",
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidRequestBody(
            HttpMessageNotReadableException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "INVALID_REQUEST_BODY"
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(
            EntityNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                "RESOURCE_NOT_FOUND"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "RESOURCE_NOT_FOUND"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                "DATA_INTEGRITY_VIOLATION"
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                "FORBIDDEN"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                "VALIDATION_ERROR"
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String detail,
            String code
    ) {
        return ResponseEntity
                .status(status)
                .body(new ApiError(detail, code));
    }
}
