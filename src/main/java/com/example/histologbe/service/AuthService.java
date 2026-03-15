package com.example.histologbe.service;

import com.example.histologbe.config.JwtProvider;
import com.example.histologbe.domain.user.User;
import com.example.histologbe.dto.user.UserLoginRequest;
import com.example.histologbe.dto.user.UserLoginResponse;
import com.example.histologbe.dto.user.UserSignUpRequest;
import com.example.histologbe.dto.user.UserSignUpResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // POST /api/auth/signup
    @Transactional
    public UserSignUpResponse signUp(UserSignUpRequest signUpRequest){
        if(userRepository.existsByUsername(signUpRequest.getUsername())){
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());
        User newUser = signUpRequest.toEntity(encodedPassword);
        User savedUser = userRepository.save(newUser);

        return UserSignUpResponse.from(savedUser);
    }

    // POST /api/auth/login
    @Transactional
    public UserLoginResponse login(UserLoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())){
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 마지막 로그인 시간 갱신
        user.setLastLoginAt(LocalDateTime.now());

        // access-token 발급
        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getUsername());
        return UserLoginResponse.from(user, accessToken);
    }


}
