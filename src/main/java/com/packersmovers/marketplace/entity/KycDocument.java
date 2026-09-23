package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.KycDocType;
import com.packersmovers.marketplace.common.util.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private KycDocType docType;

    @Column(name = "doc_url", nullable = false, length = 1000)
    private String docUrl;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}