package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findByLeadId(Long leadId);
    List<Quote> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    Optional<Quote> findByQuoteCode(String quoteCode);
}
