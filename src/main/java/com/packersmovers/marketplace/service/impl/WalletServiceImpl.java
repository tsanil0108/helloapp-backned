package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.common.enums.TransactionType;
import com.packersmovers.marketplace.common.exception.InsufficientBalanceException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;
import com.packersmovers.marketplace.dto.wallet.WalletTransactionResponse;
import com.packersmovers.marketplace.entity.Wallet;
import com.packersmovers.marketplace.entity.WalletTransaction;
import com.packersmovers.marketplace.repository.WalletRepository;
import com.packersmovers.marketplace.repository.WalletTransactionRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Wallet is the "atomic from the marketplace perspective" money layer.
 * REQUIRES_NEW + pessimistic row lock on debit/credit so the ledger can never go negative
 * or double-count under concurrent lead-unlock attempts.
 */
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AuditService auditService;

    @Override
    public WalletResponse getWallet(Long providerId) {
        Wallet wallet = walletRepository.findByProviderId(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet for provider", providerId));
        return WalletResponse.builder().providerId(providerId).balance(wallet.getBalance()).build();
    }

    @Override
    public PageResponse<WalletTransactionResponse> getTransactions(Long providerId, int page, int size) {
        Wallet wallet = walletRepository.findByProviderId(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet for provider", providerId));
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public WalletTransaction credit(Long providerId, BigDecimal amount, TransactionReferenceType referenceType,
                                     Long referenceId, String description) {
        Wallet wallet = walletRepository.findByProviderIdForUpdate(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet for provider", providerId));

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .build();
        txn = walletTransactionRepository.save(txn);

        auditService.log("WALLET_CREDIT", "Wallet", wallet.getId(), null, newBalance.toString(), description);
        return txn;
    }

    @Override
    @Transactional
    public WalletTransaction debitForUnlock(Long providerId, BigDecimal amount, Long leadId, String description) {
        Wallet wallet = walletRepository.findByProviderIdForUpdate(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet for provider", providerId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance. Current balance: " + wallet.getBalance() + ", required: " + amount);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEBIT)
                .referenceType(TransactionReferenceType.LEAD_UNLOCK)
                .referenceId(leadId)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .build();
        txn = walletTransactionRepository.save(txn);

        auditService.log("WALLET_DEBIT", "Wallet", wallet.getId(), null, newBalance.toString(), description);
        return txn;
    }

    private WalletTransactionResponse toResponse(WalletTransaction txn) {
        return WalletTransactionResponse.builder()
                .id(txn.getId())
                .type(txn.getType())
                .referenceType(txn.getReferenceType())
                .referenceId(txn.getReferenceId())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
