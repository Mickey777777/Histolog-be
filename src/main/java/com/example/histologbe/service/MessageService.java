package com.example.histologbe.service;

import com.example.histologbe.domain.chat.Chat;
import com.example.histologbe.domain.message.Message;
import com.example.histologbe.domain.message.MessageType;
import com.example.histologbe.domain.user.User;
import com.example.histologbe.dto.message.MessageRequest;
import com.example.histologbe.dto.message.MessageResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.ChatRepository;
import com.example.histologbe.repository.MessageRepository;
import com.example.histologbe.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private static final long TOKEN_LIMIT = 10_000L;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    // POST /api/chats/{chatId}/messages
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MessageResponse sendMessage(MessageRequest messageRequest, UUID chatId, UUID userId) {
        Chat chat = chatRepository.findByUserUserIdAndChatId(userId, chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getTokenUsage() >= TOKEN_LIMIT) {
            throw new CustomException(ErrorCode.TOKEN_LIMIT_EXCEEDED);
        }

        Message newUserMessage = Message.builder()
                .chat(chat)
                .message(messageRequest.getMessage())
                .messageType(MessageType.USER)
                .build();
        messageRepository.save(newUserMessage);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of(
                    "message", messageRequest.getMessage(),
                    "king", chat.getKing().name()));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
        HttpRequest aiRequest = HttpRequest.newBuilder()
                .uri(URI.create(aiServerUrl + "/histolog/ai/query"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        String content;
        Long usage;
        try {
            HttpResponse<String> aiResponse = httpClient.send(aiRequest, HttpResponse.BodyHandlers.ofString());
            content = objectMapper.readTree(aiResponse.body()).get("answer").asText();
            usage = objectMapper.readTree(aiResponse.body()).get("usage").asLong();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }

        Long userUsage = user.getTokenUsage();
        user.setTokenUsage(userUsage + usage);
        userRepository.save(user);

        Message newAssistantMessage = Message.builder()
                .chat(chat)
                .message(content)
                .messageType(MessageType.ASSISTANT)
                .build();
        Message assistantMessage = messageRepository.save(newAssistantMessage);

        return MessageResponse.from(assistantMessage);
    }

    // GET /api/chats/{chatId}/messages
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID chatId, UUID userId) {
        chatRepository.findByUserUserIdAndChatId(userId, chatId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_NOT_FOUND));

        return messageRepository.findByChatChatIdOrderByCreatedAtAsc(chatId).stream()
                .map(MessageResponse::from)
                .toList();
    }
}
