package ru.homestorage.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  // 400 - Bad Request (невалидные аргументы)
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex,
      WebRequest request) {
    log.error("Illegal argument: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  // 400 - Bad Request (наши кастомные)
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequestException(
      BadRequestException ex,
      WebRequest request) {
    log.error("Bad request: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
  }

  // 403 - Forbidden
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex,
      WebRequest request) {
    log.warn("Access denied: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request);
  }

  // 404 - Not Found
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex,
      WebRequest request) {
    log.warn("Resource not found: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
  }

  // 409 - Conflict (дубликат)
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
      DuplicateResourceException ex,
      WebRequest request) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
  }

  // 401 - Unauthorized (неверные учётные данные)
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(
      BadCredentialsException ex,
      WebRequest request) {
    log.warn("Bad credentials: {}", ex.getMessage());
    return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
  }

  // 400 - Validation errors (для @Valid)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex,
      WebRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );
    log.warn("Validation errors: {}", errors);

    ErrorResponse response = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Invalid request parameters")
        .path(request.getDescription(false))
        .details(errors)
        .build();

    return ResponseEntity.badRequest().body(response);
  }

  // 500 - Internal Server Error (все остальные ошибки)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllExceptions(
      Exception ex,
      WebRequest request) {
    log.error("Unexpected error: ", ex);
    return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  private ResponseEntity<ErrorResponse> buildErrorResponse(
      Exception ex,
      HttpStatus status,
      WebRequest request) {
    ErrorResponse response = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getDescription(false))
        .build();
    return ResponseEntity.status(status).body(response);
  }

  // Вспомогательный класс для ответа с ошибкой
  @lombok.Builder
  @lombok.Data
  public static class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;
  }
}