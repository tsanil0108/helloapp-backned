package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    @Query("""
           select coalesce(sum(t.amount), 0) from WalletTransaction t
           where t.type = com.packersmovers.marketplace.common.enums.TransactionType.DEBIT
             and t.referenceType = com.packersmovers.marketplace.common.enums.TransactionReferenceType.LEAD_UNLOCK
           """)
    BigDecimal sumLeadUnlockRevenue();
}
