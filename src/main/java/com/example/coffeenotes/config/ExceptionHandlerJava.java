package com.example.coffeenotes.config;

import com.example.coffeenotes.api.dto.error.ErrorFormatDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ExceptionHandlerJava {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerJava.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorFormatDTO> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        int statusCode = ex.getStatusCode().value();
        HttpStatus httpStatus = HttpStatus.resolve(statusCode);

        ErrorFormatDTO dto = new ErrorFormatDTO();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(statusCode);
        dto.setError(httpStatus != null ? httpStatus.getReasonPhrase() : "Unknown Status");
        dto.setMessage(ex.getReason());
        dto.setPath(request.getRequestURI());

        return ResponseEntity.status(ex.getStatusCode()).body(dto);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorFormatDTO> handleExceptionMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {

        ErrorFormatDTO dto = new ErrorFormatDTO();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(HttpStatus.BAD_REQUEST.value());
        dto.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        dto.setMessage("Malformed JSON request.");
        dto.setPath(request.getRequestURI());

        return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorFormatDTO> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        ErrorFormatDTO dto = new ErrorFormatDTO();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(HttpStatus.BAD_REQUEST.value());
        dto.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        dto.setMessage("Invalid request parameter.");
        dto.setPath(request.getRequestURI());

        return new ResponseEntity<>(dto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorFormatDTO> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorFormatDTO dto = new ErrorFormatDTO();
        dto.setTimestamp(LocalDateTime.now());
        dto.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        dto.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        dto.setMessage("Internal server error.");
        dto.setPath(request.getRequestURI());

        return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
