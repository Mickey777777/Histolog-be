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

    private List<ChatItem> chats;

    public static ChatListResponse from(List<Chat> chats) {
        return new ChatListResponse(
                chats.stream().map(ChatItem::from).collect(Collectors.toList())
        );
    }

    @Getter
    @AllArgsConstructor
    public static class ChatItem {

        @JsonProperty("chat_id")
        private UUID chatId;

        private String title;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        public static ChatItem from(Chat chat) {
            return new ChatItem(chat.getChatId(), null, chat.getCreatedAt());
        }
    }
}
