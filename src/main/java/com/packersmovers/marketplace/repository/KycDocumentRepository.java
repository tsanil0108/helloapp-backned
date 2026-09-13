package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByProviderId(Long providerId);
    boolean existsByProviderId(Long providerId);
}
