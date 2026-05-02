package com.example.histologbe.controller;

import com.example.histologbe.dto.user.*;
import com.example.histologbe.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        UserLoginResponse response = authService.refresh(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/google/initiate")
    public void initiateGoogleLogin(@RequestParam String appRedirect,
                                    HttpServletResponse response) throws IOException {
        String googleAuthUrl = authService.buildGoogleAuthUrl(appRedirect);
        response.sendRedirect(googleAuthUrl);
    }

    @GetMapping("/google/callback")
    public void handleGoogleCallback(@RequestParam String code,
                                     @RequestParam String state,
                                     HttpServletResponse response) throws IOException {
        String redirectUrl = authService.handleGoogleCallback(code, state);
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/naver/initiate")
    public void initiateNaverLogin(@RequestParam String appRedirect,
                                   HttpServletResponse response) throws IOException {
        String naverAuthUrl = authService.buildNaverAuthUrl(appRedirect);
        response.sendRedirect(naverAuthUrl);
    }

    @GetMapping("/naver/callback")
    public void handleNaverCallback(@RequestParam String code,
                                    @RequestParam String state,
                                    HttpServletResponse response) throws IOException {
        String redirectUrl = authService.handleNaverCallback(code, state);
        response.sendRedirect(redirectUrl);
    }
}
