package com.example.histologbe.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshRequest {
    @JsonProperty("refresh_token")
    @NotBlank(message = "refresh token이 필요합니다.")
    private String refreshToken;
}
