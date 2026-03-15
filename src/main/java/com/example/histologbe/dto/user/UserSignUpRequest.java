package com.example.histologbe.dto.user;

import com.example.histologbe.domain.user.User;
import com.example.histologbe.domain.user.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignUpRequest {
    @NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 주소 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .username(this.username)
                .email(this.email)
                .passwordHash(encodedPassword)
                .role(UserRole.USER)
                .build();
    }

}
