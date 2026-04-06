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

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User Not Found"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "Email is already in use"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U003", "Username is already in use"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U004", "Invalid Password"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "U005", "Invalid Google Token"),
    INVALID_NAVER_TOKEN(HttpStatus.UNAUTHORIZED, "U006", "Invalid Naver Token"),
    INVALID_REDIRECT_URI(HttpStatus.BAD_REQUEST, "U007", "Invalid Redirect URI"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
