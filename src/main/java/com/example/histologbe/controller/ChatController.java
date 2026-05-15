package com.example.histologbe.controller;

import com.example.histologbe.dto.chat.ChatCreateRequest;
import com.example.histologbe.dto.chat.ChatCreateResponse;
import com.example.histologbe.dto.chat.ChatListResponse;
import com.example.histologbe.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatCreateResponse> createChat(@Valid @RequestBody ChatCreateRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createChat(request, userId));
    }

    @GetMapping
    public ResponseEntity<ChatListResponse> getChats() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(chatService.getChats(userId));
    }
}
