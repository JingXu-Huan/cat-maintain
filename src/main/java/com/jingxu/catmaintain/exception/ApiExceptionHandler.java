package com.jingxu.catmaintain.exception;

import com.jingxu.catmaintain.dto.common.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数校验失败", details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidationException() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数校验失败", Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                details.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求参数校验失败", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson() {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "请求体不是合法的 JSON", Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "缺少请求参数",
                Map.of(exception.getParameterName(), exception.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "请求参数类型错误",
                Map.of(exception.getName(), String.valueOf(exception.getValue()))
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported() {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "不支持当前请求方法", Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "不支持当前请求内容类型", Map.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound() {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "请求的资源不存在", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation() {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "数据已存在或不满足唯一约束", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("未处理的服务器异常", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器内部错误", Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> details
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, details));
    }
}
