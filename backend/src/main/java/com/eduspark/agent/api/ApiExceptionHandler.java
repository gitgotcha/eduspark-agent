package com.eduspark.agent.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    String code = switch (status) {
      case NOT_FOUND -> "TASK_NOT_FOUND";
      case CONFLICT -> "Username already exists".equals(exception.getReason())
          ? "USERNAME_EXISTS"
          : "INVALID_TASK_STATUS";
      case UNAUTHORIZED -> "AUTHENTICATION_FAILED";
      case FORBIDDEN -> "FORBIDDEN";
      default -> "VALIDATION_FAILED";
    };
    return ResponseEntity.status(status).body(new ApiError(code, exception.getReason()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      details.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity.badRequest()
        .body(new ApiError("VALIDATION_FAILED", "Validation failed", details));
  }
}
