package com.example.histologbe.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method Not Allowed"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C003", "Access is Denied"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "Server Error"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C005", "Invalid Request Body"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C006", "Invalid Request"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}

