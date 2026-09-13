package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.entity.User;

public interface PasswordResetDeliveryService {
    void send(User user, String rawToken);
}
