package com.example.histologbe.repository;

import com.example.histologbe.domain.user.RefreshToken;
import com.example.histologbe.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    void deleteByUser(User user);
    Optional<RefreshToken> findByToken(String token);
}
