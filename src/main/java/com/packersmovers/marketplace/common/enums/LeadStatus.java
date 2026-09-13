package com.packersmovers.marketplace.common.enums;

/**
 * Lead lifecycle. Active path:
 * NEW -> VERIFIED -> MATCHED -> OFFERED -> UNLOCKED -> CONTACTED -> QUOTE_SENT -> NEGOTIATION -> CONVERTED
 * Terminal branches can occur from most active states: DUPLICATE, INVALID, EXPIRED, LOST, CANCELLED, REFUNDED.
 */
public enum LeadStatus {
    NEW,
    VERIFIED,
    DUPLICATE,      // terminal
    INVALID,        // terminal
    MATCHED,
    OFFERED,
    EXPIRED,        // terminal
    UNLOCKED,
    CONTACTED,
    QUOTE_SENT,
    NEGOTIATION,
    CONVERTED,      // terminal
    LOST,           // terminal
    CANCELLED,      // terminal
    REFUNDED;       // financial terminal

    public boolean isTerminal() {
        return this == DUPLICATE || this == INVALID || this == EXPIRED
                || this == CONVERTED || this == LOST || this == CANCELLED || this == REFUNDED;
    }
}
