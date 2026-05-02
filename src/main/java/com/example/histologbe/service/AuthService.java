package com.example.histologbe.service;

import com.example.histologbe.config.JwtProvider;
import com.example.histologbe.domain.user.AuthProvider;
import com.example.histologbe.domain.user.RefreshToken;
import com.example.histologbe.domain.user.User;
import com.example.histologbe.domain.user.UserRole;
import com.example.histologbe.dto.user.*;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.RefreshTokenRepository;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String googleClientSecret;

    @Value("${GOOGLE_CALLBACK_URI}")
    private String googleCallbackUri;

    @Value("${NAVER_CLIENT_ID}")
    private String naverClientId;

    @Value("${NAVER_CLIENT_SECRET}")
    private String naverClientSecret;

    @Value("${NAVER_CALLBACK_URI}")
    private String naverCallbackUri;

    @Value("${app.allowed-redirects}")
    private List<String> allowedRedirects;

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
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getUsername());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());


        return UserLoginResponse.from(user, accessToken, refreshToken);
    }

    // POST /api/auth/refresh
    @Transactional
    public UserLoginResponse refresh(RefreshRequest refreshRequest) {
        if(!jwtProvider.isTokenValid(refreshRequest.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        UUID requestUserId = jwtProvider.getUserId(refreshRequest.getRefreshToken());
        RefreshToken saved = refreshTokenRepository.findByToken(refreshRequest.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.EXPIRED_TOKEN));

        if(!requestUserId.equals(saved.getUser().getUserId())){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        User user = saved.getUser();
        String newAccessToken = jwtProvider.createAccessToken(user.getUserId(), user.getUsername());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getUsername());

        saved.rotate(newRefreshToken, LocalDateTime.now().plusDays(7));

        return UserLoginResponse.from(user, newAccessToken, newRefreshToken);
    }

    // GET /api/auth/google/initiate
    public String buildGoogleAuthUrl(String appRedirect) {
        validateRedirect(appRedirect);
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
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getUsername());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        String appRedirect = new String(
                Base64.getUrlDecoder().decode(state),
                StandardCharsets.UTF_8
        );
        validateRedirect(appRedirect);
        return appRedirect + "?token=" + accessToken + "&refresh_token=" + refreshToken;
    }


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
            String username = extractUsername(email, (String) payload.get("name"));

            Optional<User> existingUser = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }

            return userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .role(UserRole.USER)
                    .build());

        } catch (GeneralSecurityException | IOException e) {
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }

    // GET /api/auth/naver/initiate
    public String buildNaverAuthUrl(String appRedirect) {
        validateRedirect(appRedirect);
        String state = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(appRedirect.getBytes(StandardCharsets.UTF_8));

        return "https://nid.naver.com/oauth2.0/authorize"
                + "?response_type=code"
                + "&client_id=" + naverClientId
                + "&redirect_uri=" + URLEncoder.encode(naverCallbackUri, StandardCharsets.UTF_8)
                + "&state=" + state;
    }

    // GET /api/auth/naver/callback
    @Transactional
    @SuppressWarnings("unchecked")
    public String handleNaverCallback(String code, String state) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("code", code);
        params.add("redirect_uri", naverCallbackUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        Map<String, Object> tokenResponse = restTemplate.postForObject(
                "https://nid.naver.com/oauth2.0/token",
                request,
                Map.class
        );

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new CustomException(ErrorCode.INVALID_NAVER_TOKEN);
        }

        String providerAccessToken = (String) tokenResponse.get("access_token");

        headers = new HttpHeaders();
        headers.setBearerAuth(providerAccessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(headers);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                userInfoRequest,
                Map.class
        );

        Map<String, Object> body = userInfoResponse.getBody();
        if(body == null){
            throw new CustomException(ErrorCode.INVALID_NAVER_TOKEN);
        }

        Object tmp = body.get("response");

        if(!(tmp instanceof Map)){
            throw new CustomException(ErrorCode.INVALID_NAVER_TOKEN);
        }

        Map<?, ?> response = (Map<?, ?>) tmp;

        String providerId = (String) response.get("id");
        String email = (String) response.get("email");
        String username = extractUsername(email, (String) response.get("name"));


        User user = userRepository.findByProviderAndProviderId(AuthProvider.NAVER, providerId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .provider(AuthProvider.NAVER)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .build()));

        user.setLastLoginAt(LocalDateTime.now());

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getUsername());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), user.getUsername());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());


        String appRedirect = new String(
                Base64.getUrlDecoder().decode(state),
                StandardCharsets.UTF_8
        );
        validateRedirect(appRedirect);
        return appRedirect + "?token=" + accessToken + "&refresh_token=" + refreshToken;
    }

    private void validateRedirect(String appRedirect) {
        boolean allowed = allowedRedirects.stream()
                .anyMatch(appRedirect::startsWith);
        if (!allowed) {
            throw new CustomException(ErrorCode.INVALID_REDIRECT_URI);
        }
    }

    private String extractUsername(String email, String fallbackName) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return fallbackName;
    }
}
