package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.entity.AuditLog;
import com.packersmovers.marketplace.repository.AuditLogRepository;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Every admin change, approval, payment/refund event, unlock and status change is written here - append only. */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String action, String entityType, Long entityId, String oldValue, String newValue, String remarks) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .remarks(remarks);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            builder.actorId(principal.getUserId())
                    .actorRole(principal.getRole())
                    .actorName(principal.getFullName());
        } else {
            builder.actorRole("SYSTEM").actorName("system");
        }

        auditLogRepository.save(builder.build());
    }
}
