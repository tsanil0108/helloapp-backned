package com.packersmovers.marketplace.service;

public interface AuditService {
    void log(String action, String entityType, Long entityId, String oldValue, String newValue, String remarks);
}
