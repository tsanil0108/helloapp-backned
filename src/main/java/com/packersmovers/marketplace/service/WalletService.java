package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;
import com.packersmovers.marketplace.dto.wallet.WalletTransactionResponse;
import com.packersmovers.marketplace.entity.WalletTransaction;

import java.math.BigDecimal;

public interface WalletService {

    WalletResponse getWallet(Long providerId);

    PageResponse<WalletTransactionResponse> getTransactions(Long providerId, int page, int size);

    /** Credits a provider's wallet (e.g. after a verified Razorpay top-up, or a refund). */
    WalletTransaction credit(Long providerId, BigDecimal amount, TransactionReferenceType referenceType,
                              Long referenceId, String description);

    /**
     * Debits a provider's wallet to unlock a lead. Uses a pessimistic row lock on the wallet
     * so two concurrent unlock attempts can never double-spend the same balance.
     * Throws InsufficientBalanceException if funds are not enough.
     */
    WalletTransaction debitForUnlock(Long providerId, BigDecimal amount, Long leadId, String description);
}
