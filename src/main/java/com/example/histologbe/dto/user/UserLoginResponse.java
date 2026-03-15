package com.example.histologbe.dto.user;

import com.example.histologbe.domain.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResponse {

    private String username;

    @JsonProperty("access_token")
    private String accessToken;

    public static UserLoginResponse from(User user, String accessToken) {
        return UserLoginResponse.builder()
                .username(user.getUsername())
                .accessToken(accessToken)
                .build();
    }
}
