package com.example.histologbe.repository;

import com.example.histologbe.domain.user.AuthProvider;
import com.example.histologbe.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(UUID id);
    Optional<User> findByProviderId(String providerId);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    @Modifying
    @Query("UPDATE User u SET u.tokenUsage = 0")
    void resetAllTokenUsage();
}
