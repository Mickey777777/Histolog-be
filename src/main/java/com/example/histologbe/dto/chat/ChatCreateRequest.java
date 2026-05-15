package com.example.histologbe.dto.chat;

import com.example.histologbe.domain.chat.King;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatCreateRequest {
    @NotNull(message = "대화 상대를 선택해주세요.")
    private King king;
}
