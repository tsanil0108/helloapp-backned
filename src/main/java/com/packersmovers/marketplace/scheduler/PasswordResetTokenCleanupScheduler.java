package com.packersmovers.marketplace.scheduler;

import com.packersmovers.marketplace.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {
    private final PasswordResetTokenRepository repository;

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = repository.deleteExpiredOrOldUsed(Instant.now().minusSeconds(24 * 60 * 60));
        if (deleted > 0) log.info("Removed {} expired/old password-reset tokens", deleted);
    }
}
