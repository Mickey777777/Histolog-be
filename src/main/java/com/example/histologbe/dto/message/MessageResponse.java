package com.example.histologbe.dto.message;

import com.example.histologbe.domain.message.Message;
import com.example.histologbe.domain.message.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String message;
    private MessageType type;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .message(message.getMessage())
                .type(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
