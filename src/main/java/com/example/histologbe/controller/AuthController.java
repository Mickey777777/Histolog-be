package com.example.histologbe.controller;

import com.example.histologbe.dto.user.UserLoginRequest;
import com.example.histologbe.dto.user.UserLoginResponse;
import com.example.histologbe.dto.user.UserSignUpRequest;
import com.example.histologbe.dto.user.UserSignUpResponse;
import com.example.histologbe.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
