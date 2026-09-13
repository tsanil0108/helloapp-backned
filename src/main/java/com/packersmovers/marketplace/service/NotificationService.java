package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.common.enums.NotificationChannel;
import com.packersmovers.marketplace.entity.User;

public interface NotificationService {
    void notify(User user, String title, String message, NotificationChannel channel, String relatedEntity);
}
