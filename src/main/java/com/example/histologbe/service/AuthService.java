package com.example.histologbe.service;

import com.example.histologbe.config.JwtProvider;
import com.example.histologbe.domain.user.AuthProvider;
import com.example.histologbe.domain.user.User;
import com.example.histologbe.domain.user.UserRole;
import com.example.histologbe.dto.user.*;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;

    @Value("${GOOGLE_CALLBACK_URI}")
    private String googleCallbackUri;

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

        user.setLastLoginAt(LocalDateTime.now());

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getUsername());
        return UserLoginResponse.from(user, accessToken);
    }

    // GET /api/auth/google/initiate
    public String buildGoogleAuthUrl(String appRedirect) {
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(appRedirect.getBytes(StandardCharsets.UTF_8));
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + URLEncoder.encode(googleCallbackUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&state=" + state;
    }

    // GET /api/auth/google/callback
    @Transactional
    @SuppressWarnings("unchecked")
    public String handleGoogleCallback(String code, String state) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleCallbackUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        Map<String, Object> tokenResponse = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                request,
                Map.class
        );

        if (tokenResponse == null || !tokenResponse.containsKey("id_token")) {
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        String idToken = (String) tokenResponse.get("id_token");
        User user = processGoogleIdToken(idToken);
        user.setLastLoginAt(LocalDateTime.now());

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getUsername());

        String appRedirect = new String(
                Base64.getUrlDecoder().decode(state),
                StandardCharsets.UTF_8
        );
        return appRedirect + "?token=" + accessToken;
    }

    // id_token 검증 및 유저 생성/조회 공통 로직
    private User processGoogleIdToken(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        ).setAudience(Collections.singletonList(googleClientId)).build();

        try {
            GoogleIdToken googleIdToken = verifier.verify(idTokenString);
            if (googleIdToken == null) {
                throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String providerId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            Optional<User> existingUser = userRepository.findByProviderId(providerId);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            return userRepository.save(User.builder()
                    .username(name)
                    .email(email)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .role(UserRole.USER)
                    .build());

        } catch (GeneralSecurityException | IOException e) {
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }
}
