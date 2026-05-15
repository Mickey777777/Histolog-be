package com.example.histologbe.dto.chat;

import com.example.histologbe.domain.chat.Chat;
import com.example.histologbe.domain.chat.King;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatCreateResponse {

    @JsonProperty("chat_id")
    private UUID chatId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private King king;
    
    public static ChatCreateResponse from(Chat chat) {
        return ChatCreateResponse.builder()
                .chatId(chat.getChatId())
                .createdAt(chat.getCreatedAt())
                .king(chat.getKing())
                .build();
    }
}
