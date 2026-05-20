# Histolog Backend - OAuth & 인증 흐름

> Spring Security STATELESS + 자체 JWT(access/refresh) + Google/Naver OAuth2

## 1. 인증 아키텍처 한 줄 요약

외부 OAuth로는 **사용자 확인만** 하고, 클라이언트와의 통신은 **자체 발급 JWT(access + refresh)** 로 한다. RefreshToken은 DB에 저장해 rotate · 무효화한다.

| 토큰 | 만료 | 저장 위치 |
|---|---|---|
| Access Token | 30분 | 클라이언트만 보관 (DB 저장 X) |
| Refresh Token | 7일 | `refresh_tokens` 테이블에 user당 1개 |

핵심 컴포넌트:

- `JwtProvider` — HMAC-SHA로 서명, subject=`userId(UUID)`, claim `username`
- `JwtAuthenticationFilter` — 매 요청에서 Authorization 헤더 파싱 → `SecurityContext`에 `userId` 세팅
- `AuthService` — 회원가입/로그인/리프레시/Google·Naver 콜백 처리
- `SecurityConfig` — `/api/auth/**` permitAll, 그 외 authenticated, CSRF off, STATELESS

---

## 2. 전체 요청 인증 흐름 (보호 API 호출 시)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant SCH as SecurityContextHolder
    participant Ctrl as Controller
    participant Svc as Service

    C->>F: Request + Authorization: Bearer <accessToken>
    alt 헤더 없음 / Bearer 아님
        F->>Ctrl: 통과 (인증 미설정)
        Ctrl-->>C: 401 (anyRequest().authenticated)
    else 토큰 있음
        F->>F: jwtProvider.isTokenValid(token)
        alt valid
            F->>SCH: setAuthentication(userId)
            F->>Ctrl: forward
            Ctrl->>Svc: SecurityContextHolder...principal == UUID userId
            Svc-->>Ctrl: 처리 결과
            Ctrl-->>C: 200 OK
        else invalid/expired
            F->>Ctrl: 통과 (인증 미설정)
            Ctrl-->>C: 401
        end
    end
```

---

## 3. 로컬 회원가입 / 로그인 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant A as AuthController
    participant S as AuthService
    participant UR as UserRepository
    participant RR as RefreshTokenRepository
    participant J as JwtProvider

    Note over C,A: 회원가입
    C->>A: POST /api/auth/signup {username,email,password}
    A->>S: signUp(req)
    S->>UR: existsByUsername / existsByEmail
    alt 중복
        S-->>A: throw U002 / U003
        A-->>C: 409 Conflict
    else 통과
        S->>S: BCryptPasswordEncoder.encode(password)
        S->>UR: save(User{provider=LOCAL, role=USER})
        S-->>A: UserSignUpResponse
        A-->>C: 201 Created
    end

    Note over C,A: 로그인
    C->>A: POST /api/auth/login {username,password}
    A->>S: login(req)
    S->>UR: findByUsername
    alt 없음
        S-->>C: 401 U001
    end
    S->>S: passwordEncoder.matches?
    alt 불일치
        S-->>C: 401 U004
    end
    S->>UR: user.lastLoginAt = now
    S->>J: createAccessToken(userId, username)
    S->>J: createRefreshToken(userId, username)
    S->>RR: deleteByUser(user)
    S->>RR: save(RefreshToken{token, expiresAt=now+7d})
    S-->>A: UserLoginResponse{username, access_token, refresh_token}
    A-->>C: 200 OK
```

---

## 4. Refresh Token Rotate 흐름

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant A as AuthController
    participant S as AuthService
    participant J as JwtProvider
    participant RR as RefreshTokenRepository

    C->>A: POST /api/auth/refresh {refresh_token}
    A->>S: refresh(req)
    S->>J: isTokenValid(refresh_token)
    alt 서명/형식 invalid
        S-->>C: 401 A001
    end
    S->>J: getUserId(refresh_token)  ->  requestUserId
    S->>RR: findByToken(refresh_token)
    alt DB에 없음
        S-->>C: 401 A002
    end
    S->>S: expiresAt < now ?
    alt 만료
        S-->>C: 401 A002
    end
    S->>S: requestUserId == saved.user.userId ?
    alt 불일치
        S-->>C: 401 A001
    end
    S->>J: createAccessToken / createRefreshToken (new)
    S->>RR: saved.rotate(newRefreshToken, now+7d)
    S-->>A: UserLoginResponse(new tokens)
    A-->>C: 200 OK
```

rotate 후에도 **동일 row를 업데이트**하므로, 도난된 옛 refresh token은 즉시 무효가 된다.

---

## 5. Google OAuth 흐름

Authorization Code Flow + `id_token` 검증 방식.

```mermaid
sequenceDiagram
    autonumber
    participant App as Client App
    participant BE as Histolog BE
    participant G as Google OAuth
    participant DB as DB

    App->>BE: GET /api/auth/google/initiate?appRedirect=histolog://callback
    BE->>BE: validateRedirect(appRedirect)  (allowed-redirects)
    BE->>BE: state = base64url(appRedirect)
    BE-->>App: 302 → https://accounts.google.com/o/oauth2/v2/auth?... &state=...

    App->>G: 사용자 로그인 + 동의
    G-->>App: 302 → GOOGLE_CALLBACK_URI?code=...&state=...

    App->>BE: GET /api/auth/google/callback?code=...&state=...
    BE->>G: POST oauth2.googleapis.com/token {code, client_id, client_secret, redirect_uri, grant_type}
    G-->>BE: { id_token, access_token, ... }
    BE->>BE: GoogleIdTokenVerifier.verify(id_token, audience=GOOGLE_CLIENT_ID)
    alt 검증 실패
        BE-->>App: 401 U005
    end
    BE->>DB: findByProviderAndProviderId(GOOGLE, sub)
    alt 신규
        BE->>DB: save(User{provider=GOOGLE, providerId=sub, role=USER})
    end
    BE->>BE: createAccessToken / createRefreshToken (자체 JWT)
    BE->>DB: deleteByUser → save(RefreshToken)
    BE->>BE: appRedirect = decode(state)  + validate again
    BE-->>App: 302 → {appRedirect}?token={access}&refresh_token={refresh}
    App->>App: URL에서 토큰 추출 → 보관
```

### 보안 포인트

- `appRedirect` 는 화이트리스트(`app.allowed-redirects`) prefix 매칭으로 검증. **callback 진입 후에도 다시 검증**해 state 변조에 대비.
- `state` 는 단순 base64로만 인코딩되어 있어 **위조 방지/리플레이 방지용 nonce가 아님** — appRedirect 전달 용도. CSRF 방어가 추가로 필요하다면 random nonce + 서버 측 저장이 필요.
- 토큰을 URL 쿼리스트링으로 전달 → 브라우저 history/로그에 남을 수 있음. 모바일/네이티브 deep-link 사용을 전제로 한 설계.

---

## 6. Naver OAuth 흐름

Google과 다르게 **id_token이 없어 access_token으로 userinfo API를 한 번 더 호출**한다.

```mermaid
sequenceDiagram
    autonumber
    participant App as Client App
    participant BE as Histolog BE
    participant N as Naver OAuth
    participant U as openapi.naver.com
    participant DB as DB

    App->>BE: GET /api/auth/naver/initiate?appRedirect=...
    BE->>BE: validateRedirect + state=base64url(appRedirect)
    BE-->>App: 302 → https://nid.naver.com/oauth2.0/authorize?... &state

    App->>N: 로그인 + 동의
    N-->>App: 302 → NAVER_CALLBACK_URI?code=...&state=...

    App->>BE: GET /api/auth/naver/callback?code=...&state=...
    BE->>N: POST nid.naver.com/oauth2.0/token (grant_type=authorization_code)
    N-->>BE: { access_token, ... }
    BE->>U: GET /v1/nid/me  (Authorization: Bearer access_token)
    U-->>BE: { response: { id, email, name, ... } }
    alt response 누락
        BE-->>App: 401 U006
    end
    BE->>DB: findByProviderAndProviderId(NAVER, id)
    alt 신규
        BE->>DB: save(User{provider=NAVER, providerId=id, role=USER})
    end
    BE->>BE: createAccessToken / createRefreshToken
    BE->>DB: deleteByUser → save(RefreshToken)
    BE-->>App: 302 → appRedirect?token=...&refresh_token=...
```

---

## 7. 사용자 식별 규칙 (provider별)

| Provider | `providerId` 출처 | `username` 산출 |
|---|---|---|
| LOCAL | (사용 안 함) | 회원가입 입력값 |
| GOOGLE | `id_token.sub` | `email`의 `@` 앞부분, 없으면 `payload.name` |
| NAVER | `response.id` | `email`의 `@` 앞부분, 없으면 `response.name` |

> 동일 이메일이라도 provider가 다르면 다른 User로 취급된다. (`(provider, providerId)` 조회 기준)

---

## 8. 보호 경로 정책 요약

```text
SecurityFilterChain:
  cors                     → 모든 origin pattern 허용, credentials 허용
  csrf                     → disable (REST + JWT 전제)
  sessionCreationPolicy    → STATELESS
  /api/auth/**             → permitAll
  그 외                     → authenticated
  filter chain             → JwtAuthenticationFilter @ before UsernamePasswordAuthenticationFilter
```

토큰이 무효해도 `JwtAuthenticationFilter` 자체에서 401 응답을 만들어 보내지는 않는다. `SecurityContext`만 비워둔 채 다음 필터로 넘기고, 최종적으로 `authorizeHttpRequests` 규칙에서 401/403이 나간다.
