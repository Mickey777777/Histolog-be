package com.example.histologbe.service;

import com.example.histologbe.domain.chat.Chat;
import com.example.histologbe.domain.user.User;
import com.example.histologbe.dto.chat.ChatCreateResponse;
import com.example.histologbe.dto.chat.ChatListResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.ChatRepository;
import com.example.histologbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    // POST /api/chats
    @Transactional
    public ChatCreateResponse createChat(UUID userId){
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Chat newChat = Chat.builder()
                .user(user)
                .build();

        Chat savedChat = chatRepository.save(newChat);

        return ChatCreateResponse.from(savedChat);
    }

    // GET /api/chats
    @Transactional(readOnly = true)
    public ChatListResponse getChats(UUID userId){
        return ChatListResponse.from(chatRepository.findByUserUserIdOrderByCreatedAtDesc(userId));
    }
}
