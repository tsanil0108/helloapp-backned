package com.packersmovers.marketplace.dto.wallet;

import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long id;
    private TransactionType type;
    private TransactionReferenceType referenceType;
    private Long referenceId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Instant createdAt;
}
