package com.packersmovers.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        boolean firebaseEnabled,
        boolean smsEnabled,
        boolean whatsappEnabled,
        boolean emailEnabled
) {
}
