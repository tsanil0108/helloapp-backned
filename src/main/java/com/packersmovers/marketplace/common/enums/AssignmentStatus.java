package com.packersmovers.marketplace.common.enums;

public enum AssignmentStatus {

    OFFERED,
    VIEWED,

    UNLOCKED,
    CONTACTED,

    QUOTE_SENT,
    NEGOTIATION,

    BOOKED,
    SERVICE_IN_PROGRESS,
    COMPLETED,

    LOST,
    INVALID,
    DUPLICATE,
    EXPIRED,
    CANCELLED,
    REFUNDED;

    public boolean isTerminal() {
        return this == COMPLETED
                || this == LOST
                || this == INVALID
                || this == DUPLICATE
                || this == EXPIRED
                || this == CANCELLED
                || this == REFUNDED;
    }
}