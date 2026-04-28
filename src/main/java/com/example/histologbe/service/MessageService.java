package com.example.histologbe.service;

import com.example.histologbe.domain.chat.Chat;
import com.example.histologbe.domain.message.Message;
import com.example.histologbe.domain.message.MessageType;
import com.example.histologbe.dto.message.MessageRequest;
import com.example.histologbe.dto.message.MessageResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.ChatRepository;
import com.example.histologbe.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    // POST /api/chats/{chatId}/messages
    @Transactional
    public MessageResponse sendMessage(MessageRequest messageRequest, UUID chatId, UUID userId) {
        Chat chat = chatRepository.findByUserUserIdAndChatId(userId, chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        Message newUserMessage = Message.builder()
                .chat(chat)
                .message(messageRequest.getMessage())
                .messageType(MessageType.USER)
                .build();
        messageRepository.save(newUserMessage);

        // TODO AI서버를 통해서 응답 얻은다음 리턴

        String content = "ok";
        Message newAssistantMessage = Message.builder()
                .chat(chat)
                .message(content)
                .messageType(MessageType.ASSISTANT)
                .build();
        Message assistantMessage = messageRepository.save(newAssistantMessage);

        return MessageResponse.from(assistantMessage);
    }
}
