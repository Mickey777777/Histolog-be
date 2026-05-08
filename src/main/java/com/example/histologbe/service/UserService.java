package com.example.histologbe.service;

import com.example.histologbe.domain.user.User;
import com.example.histologbe.dto.user.UsageResponse;
import com.example.histologbe.exception.CustomException;
import com.example.histologbe.exception.ErrorCode;
import com.example.histologbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void resetTokenUsage() {
        userRepository.resetAllTokenUsage();
    }

    // GET /api/user/usage
    public UsageResponse getUsage(UUID userId){
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UsageResponse.from(user.getTokenUsage());
    }
}
