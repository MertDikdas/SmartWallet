package com.smartwallet.budgetservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BudgetNotFoundException.class)
    public ResponseEntity<ApiError> handleBudgetNotFound(
            BudgetNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError error = createError(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleBudgetAlreadyExists(
            BudgetAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        ApiError error = createError(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request,
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ApiError error = createError(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    private ApiError createError(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
    }
}