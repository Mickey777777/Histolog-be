# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle wrapper & 빌드 파일 먼저 복사 (의존성 캐싱)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

# 의존성 다운로드 (소스 변경 시 캐시 재사용)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# 소스 코드 복사 & 빌드 (테스트 제외)
COPY src src
COPY .env .

RUN ./gradlew bootJar -x test --no-daemon

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# spring-dotenv가 .env를 읽을 수 있도록 복사
COPY .env .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
