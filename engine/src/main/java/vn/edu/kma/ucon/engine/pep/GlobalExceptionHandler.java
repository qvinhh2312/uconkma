package vn.edu.kma.ucon.engine.pep;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global API exception mapping for technical failures that are not standard
 * policy denies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(ObjectOptimisticLockingFailureException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "RACE_CONDITION",
                "Co xung dot optimistic locking o buoc commit, nghia la state da thay doi do request dong thoi khac.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_REGISTRATION",
                "Database phat hien ban ghi dang ky trung lap tai buoc commit nen giao dich bi tu choi.",
                request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleInvariantViolation(IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVARIANT_VIOLATION", ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request != null ? request.getRequestURI() : null);
        return ResponseEntity.status(status).body(response);
    }
}
