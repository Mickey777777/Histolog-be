package com.example.histologbe.dto.user;

import com.example.histologbe.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignUpResponse {

    private String username;
    private String email;

    public static UserSignUpResponse from(User user) {
        return UserSignUpResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
