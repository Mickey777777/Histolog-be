package com.example.histologbe.repository;

import com.example.histologbe.domain.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChatChatIdOrderByCreatedAtAsc(UUID chatId);

}