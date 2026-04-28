package com.example.histologbe.controller;

import com.example.histologbe.config.JwtProvider;
import com.example.histologbe.dto.message.MessageRequest;
import com.example.histologbe.dto.message.MessageResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class MessageController {

    private final JwtProvider jwtProvider;
    private final MessageService messageService;

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID chatId,
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody MessageRequest request
    ){
        if(!authorization.startsWith("Bearer ")){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String token = authorization.substring(7);
        if(!jwtProvider.isTokenValid(token)){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        UUID userId = jwtProvider.getUserId(token);

        return ResponseEntity.ok(messageService.sendMessage(request, chatId, userId));
    }

}
