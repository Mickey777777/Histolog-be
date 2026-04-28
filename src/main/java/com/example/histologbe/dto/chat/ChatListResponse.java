package com.example.histologbe.dto.chat;

import com.example.histologbe.domain.chat.Chat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class ChatListResponse {

    private List<SessionItem> sessions;

    public static ChatListResponse from(List<Chat> chats) {
        return new ChatListResponse(
                chats.stream().map(SessionItem::from).collect(Collectors.toList())
        );
    }

    @Getter
    @AllArgsConstructor
    public static class SessionItem {

        @JsonProperty("session_id")
        private UUID sessionId;

        private String title;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public static SessionItem from(Chat chat) {
            return new SessionItem(chat.getChatId(), null, chat.getCreatedAt());
        }
    }
}
