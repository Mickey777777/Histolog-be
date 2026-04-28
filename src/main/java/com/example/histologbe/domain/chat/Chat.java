package com.example.histologbe.domain.chat;

import com.example.histologbe.domain.BaseTimeEntity;
import com.example.histologbe.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(name = "chats")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat extends BaseTimeEntity {

    @Id
    @Column(name = "chat_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
