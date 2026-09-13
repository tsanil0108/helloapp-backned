package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.entity.User;
import com.packersmovers.marketplace.service.PasswordResetDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetDeliveryServiceImpl implements PasswordResetDeliveryService {
    private final JavaMailSender mailSender;

    @Value("${app.password-reset.frontend-url}")
    private String frontendUrl;

    @Value("${app.password-reset.mail-from:}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Override
    public void send(User user, String rawToken) {
        if (!StringUtils.hasText(user.getEmail()) || !StringUtils.hasText(mailHost)) {
            log.info("Password reset requested for userId={} but email delivery is not configured", user.getId());
            return;
        }

        String link = frontendUrl + (frontendUrl.contains("?") ? "&" : "?") + "token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Packers & Movers account password");
        message.setText("We received a password reset request.\n\n"
                + "Reset your password using this secure link:\n" + link + "\n\n"
                + "This link expires in 30 minutes and can be used only once.\n"
                + "If you did not request this, you can safely ignore this email.");
        mailSender.send(message);
    }
}
