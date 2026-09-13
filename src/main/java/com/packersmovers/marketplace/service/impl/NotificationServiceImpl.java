package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.NotificationChannel;
import com.packersmovers.marketplace.config.NotificationProperties;
import com.packersmovers.marketplace.entity.Notification;
import com.packersmovers.marketplace.entity.User;
import com.packersmovers.marketplace.repository.NotificationRepository;
import com.packersmovers.marketplace.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Persists an in-app notification record and, when the corresponding provider toggle is on
 * (app.notifications.*). In-app notifications are persisted immediately.
 * External push/SMS/WhatsApp channels remain provider adapters and must be wired to real credentials
 * before those channels are marked as delivered.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;

    @Override
    @Async
    public void notify(User user, String title, String message, NotificationChannel channel, String relatedEntity) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .channel(channel)
                .relatedEntity(relatedEntity)
                .sentAt(Instant.now())
                .deliveryStatus(isChannelEnabled(channel) ? "PERSISTED_PENDING_DISPATCH" : "SKIPPED_DISABLED")
                .build();
        notificationRepository.save(notification);

        if (isChannelEnabled(channel)) {
            // External provider adapters must be configured before this can be marked delivered.
            log.info("Notification persisted for user={} channel={} title='{}'",
                    user != null ? user.getId() : "n/a", channel, title);
        } else {
            log.debug("Notification channel {} disabled - recorded only.", channel);
        }
    }

    private boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case PUSH -> notificationProperties.firebaseEnabled();
            case SMS -> notificationProperties.smsEnabled();
            case WHATSAPP -> notificationProperties.whatsappEnabled();
            case EMAIL -> notificationProperties.emailEnabled();
            case IN_APP -> true;
        };
    }
}
