package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByProviderId(Long providerId);

    /** Pessimistic lock to serialize concurrent unlock/topup attempts on the same wallet. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.provider.id = :providerId")
    Optional<Wallet> findByProviderIdForUpdate(@Param("providerId") Long providerId);
}
