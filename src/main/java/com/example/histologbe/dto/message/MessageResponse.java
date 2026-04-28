package com.example.histologbe.dto.message;

import com.example.histologbe.domain.message.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private String message;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .message(message.getMessage())
                .build();
    }
}
