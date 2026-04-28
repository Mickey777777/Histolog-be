package com.example.histologbe.controller;

import com.example.histologbe.config.JwtProvider;
import com.example.histologbe.dto.chat.ChatCreateResponse;
import com.example.histologbe.dto.chat.ChatListResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
    private final JwtProvider jwtProvider;
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatCreateResponse> createChat(
            @RequestHeader("Authorization") String authorization
    ){
        if(!authorization.startsWith("Bearer ")){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String token = authorization.substring(7);
        if(!jwtProvider.isTokenValid(token)){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        UUID userId = jwtProvider.getUserId(token);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createChat(userId));
    }

    @GetMapping
    public ResponseEntity<ChatListResponse> getChats(
            @RequestHeader("Authorization") String authorization
    ){
        if(!authorization.startsWith("Bearer ")){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String token = authorization.substring(7);
        if(!jwtProvider.isTokenValid(token)){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        UUID userId = jwtProvider.getUserId(token);

        return ResponseEntity.ok(chatService.getChats(userId));
    }
}
