package com.packersmovers.marketplace.common.enums;

public enum TransactionType {
    CREDIT,      // money added to wallet
    DEBIT,       // money removed from wallet (e.g. lead unlock)
    REFUND,      // refund credited back
    ADJUSTMENT   // manual admin correction
}
