package com.example.histologbe.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageResponse {

    @JsonProperty("token_usage")
    private Long tokenUsage;

    public static UsageResponse from(Long tokenUsage) {
        return UsageResponse.builder()
                .tokenUsage(tokenUsage)
                .build();
    }
}
