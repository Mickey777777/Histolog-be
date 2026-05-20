# Histolog Backend - API 문서

> Spring Boot 3.5.8 / Java 17 / Oracle DB / Stateless JWT 인증
> Base URL (개발): `http://localhost:8080`

## 1. 개요

| 항목 | 내용 |
|---|---|
| Base Path | `/api` |
| 인증 방식 | Bearer JWT (Authorization 헤더) |
| 공개 경로 | `/api/auth/**` (Security 설정상 permitAll) |
| 보호 경로 | 그 외 모든 `/api/**` 요청은 인증 필수 |
| 세션 정책 | STATELESS (서버 세션 없음) |
| CORS | 모든 Origin pattern 허용, credentials 허용 |
| AccessToken 만료 | 1,800,000 ms (30분) |
| RefreshToken 만료 | 604,800,000 ms (7일) |

### 인증 헤더 형식

```
Authorization: Bearer <access_token>
```

`JwtAuthenticationFilter`가 헤더의 토큰을 검증하고, `SecurityContext`의 principal을 `UUID userId`로 채워준다. 컨트롤러에서는 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`로 꺼내 사용.

---

## 2. 공통 응답 - 에러 포맷

모든 비즈니스 에러는 `GlobalExceptionHandler`를 거쳐 다음 형식으로 반환된다.

```json
{
  "code": "U001",
  "message": "User Not Found",
  "errors": []
}
```

`MethodArgumentNotValidException`(밸리데이션 실패) 발생 시 `errors` 배열에 필드 단위 오류가 채워진다.

```json
{
  "code": "C001",
  "message": "Invalid Input Value",
  "errors": [
    { "field": "string", "value": "string", "reason": "string" }
  ]
}
```

### 에러 코드 표

| Code | HTTP | 의미 |
|---|---|---|
| C001 | 400 | Invalid Input Value (validation 실패) |
| C002 | 405 | Method Not Allowed |
| C003 | 403 | Access is Denied |
| C004 | 500 | Server Error |
| C005 | 400 | Invalid Request Body |
| C006 | 400 | Invalid Request |
| U001 | 404 | User Not Found |
| U002 | 409 | Email is already in use |
| U003 | 409 | Username is already in use |
| U004 | 401 | Invalid Password |
| U005 | 401 | Invalid Google Token |
| U006 | 401 | Invalid Naver Token |
| U007 | 400 | Invalid Redirect URI |
| A001 | 401 | Invalid or missing token |
| A002 | 401 | Expired Token |
| M001 | 404 | Chat Not Found |
| AI001 | 500 | AI Server Error |
| T001 | 403 | Token limit exceeded |

---

## 3. Auth API (`/api/auth`) — 인증 불필요

### 3.1 POST `/api/auth/signup` — 로컬 회원가입

- **권한**: Public
- **Request Body**

```json
{
  "username": "string",
  "email": "user@example.com",
  "password": "string"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| username | string | NotBlank |
| email | string | NotBlank, Email 형식 |
| password | string | NotBlank |

- **Response 201 Created**

```json
{
  "username": "string",
  "email": "user@example.com"
}
```

- **에러**: `U002` (이메일 중복), `U003` (username 중복), `C001` (validation 실패)

### 3.2 POST `/api/auth/login` — 로컬 로그인

- **권한**: Public
- **Request Body**

```json
{ "username": "string", "password": "string" }
```

- **Response 200 OK**

```json
{
  "username": "string",
  "access_token": "string",
  "refresh_token": "string"
}
```

- **부수효과**: `users.last_login_at` 갱신, 기존 refresh token 삭제 후 새 토큰 저장 (7일 만료).
- **에러**: `U001`(사용자 없음), `U004`(비번 불일치)

### 3.3 POST `/api/auth/refresh` — 토큰 재발급

- **권한**: Public (refresh token 자체가 자격증명)
- **Request Body**

```json
{ "refresh_token": "string" }
```

- **Response 200 OK**: 새 access/refresh 토큰. 기존 RefreshToken 레코드는 rotate되어 동일 row의 token 컬럼만 갱신.
- **에러**: `A001`(토큰 무효/소유자 불일치), `A002`(만료/DB 미존재)

### 3.4 GET `/api/auth/google/initiate` — Google OAuth 시작

- **권한**: Public
- **Query Param**: `appRedirect` (string, 필수) — 로그인 후 토큰을 붙여 돌려보낼 클라이언트 URL. `app.allowed-redirects` 화이트리스트 검증.
- **응답**: 302 Redirect → `https://accounts.google.com/o/oauth2/v2/auth?...`
  - `state` 파라미터에 base64url(appRedirect)을 실어 보냄.
- **에러**: `U007`(허용되지 않은 redirect)

### 3.5 GET `/api/auth/google/callback` — Google OAuth 콜백

- **권한**: Public (Google이 호출)
- **Query Param**: `code`, `state`
- **처리 흐름**
  1. `code`를 Google `oauth2.googleapis.com/token`에 교환 → `id_token` 획득
  2. `GoogleIdTokenVerifier`로 `id_token` 검증 (audience = `GOOGLE_CLIENT_ID`)
  3. `(provider=GOOGLE, providerId=sub)` 으로 user upsert
  4. 자체 JWT access/refresh 발급, RefreshToken row 갱신
  5. 클라이언트 redirect: `appRedirect?token={access}&refresh_token={refresh}`
- **응답**: 302 Redirect
- **에러**: `U005`(id_token 검증 실패), `U007`(redirect 검증 실패)

### 3.6 GET `/api/auth/naver/initiate` — Naver OAuth 시작

- 동작은 Google과 동일. 응답: `https://nid.naver.com/oauth2.0/authorize?...`로 302.

### 3.7 GET `/api/auth/naver/callback` — Naver OAuth 콜백

- **처리 흐름**: Google과 거의 같지만, Naver는 id_token이 아닌 **access_token으로 `openapi.naver.com/v1/nid/me`를 호출**해 사용자 정보를 가져온다. 이후 자체 JWT 발급은 동일.
- **에러**: `U006`(token/userinfo 실패), `U007`(redirect 검증 실패)

---

## 4. User API (`/api/user`) — 인증 필요

### 4.1 GET `/api/user/usage` — 토큰 사용량 조회

- **권한**: 인증된 사용자
- **Response 200 OK**

```json
{ "token_usage": 1234 }
```

- 백엔드 cron(`0 0 */2 * * *`)이 모든 사용자의 `token_usage`를 주기적으로 0으로 리셋.
- **에러**: `U001`

---

## 5. Chat API (`/api/chats`) — 인증 필요

### 5.1 POST `/api/chats` — 새 채팅 생성

- **Request Body**

```json
{ "king": "JEONGJO" }
```

| 필드 | 타입 | 제약 |
|---|---|---|
| king | enum (`JEONGJO`, `DANJONG`) | NotNull |

- **Response 201 Created**

```json
{
  "chat_id": "uuid",
  "created_at": "2026-05-20T12:34:56",
  "king": "JEONGJO"
}
```

- **에러**: `U001`, `C001`

### 5.2 GET `/api/chats` — 사용자의 채팅 목록

- **Response 200 OK** — 최신순(`createdAt DESC`)

```json
{
  "chats": [
    { "chat_id": "uuid", "title": null, "created_at": "2026-05-20T12:34:56" }
  ]
}
```

> 참고: 현재 `title` 필드는 항상 `null` 로 직렬화된다(DTO 변환 시 하드코딩). 추후 첫 메시지 요약 등으로 채울 여지.

---

## 6. Message API (`/api/chats/{chatId}/messages`) — 인증 필요

### 6.1 GET `/api/chats/{chatId}/messages` — 메시지 목록

- **Path Param**: `chatId` (UUID)
- **Response 200 OK** — `createdAt ASC`

```json
[
  {
    "message": "string",
    "type": "USER",
    "createdAt": "2026-05-20T12:35:00"
  },
  {
    "message": "string",
    "type": "ASSISTANT",
    "createdAt": "2026-05-20T12:35:02"
  }
]
```

- **에러**: `M001` (해당 chat이 본인 소유가 아니거나 존재하지 않음)

### 6.2 POST `/api/chats/{chatId}/messages` — 메시지 전송 (AI 응답 받기)

- **Path Param**: `chatId` (UUID)
- **Request Body**

```json
{ "message": "string" }
```

- **처리 흐름**
  1. chat 소유권 검증 → `M001`
  2. user `token_usage >= 10_000` 이면 `T001`
  3. 사용자 메시지 저장 (`type=USER`)
  4. AI 서버 호출: `POST ${ai.server.url}/histolog/ai/query`, body `{ message, king }`
  5. 응답 `{ answer, usage }` 파싱 → `user.token_usage += usage`
  6. assistant 메시지 저장 → DTO 반환
- **Response 200 OK**

```json
{
  "message": "string",
  "type": "ASSISTANT",
  "createdAt": "2026-05-20T12:35:02"
}
```

- **에러**: `M001`, `U001`, `T001`, `AI001`
- **트랜잭션**: `Propagation.NOT_SUPPORTED` — AI 호출 동안 DB 커넥션을 잡지 않기 위해 트랜잭션 밖에서 실행됨. 사용자 메시지/assistant 메시지/usage 갱신은 각 save 호출 시점에 즉시 커밋된다는 점에 주의.

---

## 7. 엔드포인트 요약

| Method | Path | 인증 | 요약 |
|---|---|---|---|
| POST | /api/auth/signup | ❌ | 로컬 회원가입 |
| POST | /api/auth/login | ❌ | 로컬 로그인 |
| POST | /api/auth/refresh | ❌ | 토큰 재발급 |
| GET | /api/auth/google/initiate | ❌ | Google OAuth 시작 (302) |
| GET | /api/auth/google/callback | ❌ | Google OAuth 콜백 (302) |
| GET | /api/auth/naver/initiate | ❌ | Naver OAuth 시작 (302) |
| GET | /api/auth/naver/callback | ❌ | Naver OAuth 콜백 (302) |
| GET | /api/user/usage | ✅ | 토큰 사용량 조회 |
| POST | /api/chats | ✅ | 채팅 생성 |
| GET | /api/chats | ✅ | 내 채팅 목록 |
| GET | /api/chats/{chatId}/messages | ✅ | 메시지 목록 |
| POST | /api/chats/{chatId}/messages | ✅ | 메시지 전송 + AI 응답 |

---

## 8. 환경 변수 / 설정

| 키 | 설명 |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Oracle DB 접속 |
| `JWT_SECRET` | HMAC-SHA용 시크릿 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_CALLBACK_URI` | Google OAuth |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `NAVER_CALLBACK_URI` | Naver OAuth |
| `AI_SERVER_URL` | AI 서버 base URL (기본 `http://127.0.0.1:8000`) |
| `app.allowed-redirects` | OAuth 후 클라이언트 redirect 화이트리스트 |
