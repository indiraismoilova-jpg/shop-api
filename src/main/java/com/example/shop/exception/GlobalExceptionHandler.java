package com.example.shop.exception;

import com.example.shop.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        HttpServletRequest servletRequest =
                ((ServletWebRequest) request).getRequest();

        Map<String, List<String>> details = new LinkedHashMap<>();
        String message = defaultMessage(status.value());

        if (exception instanceof MethodArgumentNotValidException validation) {
            message = "Validation failed";

            validation.getBindingResult().getFieldErrors()
                    .forEach(error -> details
                            .computeIfAbsent(
                                    error.getField(),
                                    key -> new ArrayList<>())
                            .add(error.getDefaultMessage() == null
                                    ? "Invalid value"
                                    : error.getDefaultMessage()));
        } else if (exception instanceof ResponseStatusException responseStatus
                && !status.is5xxServerError()
                && responseStatus.getReason() != null) {

            message = responseStatus.getReason();
        }

        return new ResponseEntity<>(
                createBody(
                        status.value(),
                        message,
                        servletRequest.getRequestURI(),
                        details),
                headers,
                status);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {

        return respond(
                401,
                "Authentication required or invalid credentials",
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return respond(403, "Access denied", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        return respond(
                409,
                "Data conflicts with an existing record or relation",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unexpected request failure", exception);

        return respond(500, "Internal server error", request);
    }
    @ExceptionHandler({
            ResourceNotFoundException.class,
            UnauthorizedException.class,
            ForbiddenException.class,
            InsufficientStockException.class,
            PaymentFailedException.class,
            OrderCancellationException.class,
            PromoCodeInvalidException.class,
            ReturnNotAllowedException.class,
            RateLimitExceededException.class
    })
    public ResponseEntity<Object> handleBusinessException(
            RuntimeException exception,
            HttpServletRequest request) {

        int status = switch (exception) {
            case ResourceNotFoundException ignored -> 404;
            case UnauthorizedException ignored -> 401;
            case ForbiddenException ignored -> 403;
            case InsufficientStockException ignored -> 409;
            case PaymentFailedException ignored -> 402;
            case OrderCancellationException ignored -> 409;
            case PromoCodeInvalidException ignored -> 400;
            case ReturnNotAllowedException ignored -> 409;
            case RateLimitExceededException ignored -> 429;
            default -> 500;
        };

        String message = exception.getMessage() == null
                ? defaultMessage(status)
                : exception.getMessage();

        return respond(status, message, request);
    }
    private ResponseEntity<Object> respond(
            int status,
            String message,
            HttpServletRequest request) {

        return ResponseEntity.status(status).body(
                createBody(
                        status,
                        message,
                        request.getRequestURI(),
                        Map.of()));
    }

    private ApiErrorResponse createBody(
            int status,
            String message,
            String path,
            Map<String, List<String>> details) {

        HttpStatus httpStatus = HttpStatus.resolve(status);

        return new ApiErrorResponse(
                Instant.now(),
                status,
                httpStatus == null ? "UNKNOWN" : httpStatus.name(),
                message,
                path,
                details);
    }

    private String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "Invalid request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 405 -> "HTTP method not allowed";
            case 406 -> "Requested response format is not supported";
            case 409 -> "Request conflicts with current state";
            case 413 -> "Request is too large";
            case 415 -> "Content type is not supported";
            case 429 -> "Too many requests";
            default -> status >= 500
                    ? "Internal server error"
                    : "Request failed";
        };
    }
}