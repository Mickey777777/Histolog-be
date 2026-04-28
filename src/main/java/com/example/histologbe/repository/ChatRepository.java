package com.example.histologbe.repository;

import com.example.histologbe.domain.chat.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    List<Chat> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

}
