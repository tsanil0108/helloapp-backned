package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.ProviderStatus;
import com.packersmovers.marketplace.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUserId(Long userId);

    List<Provider> findByStatus(ProviderStatus status);

    long countByStatus(ProviderStatus status);

    long countByStatusIn(List<ProviderStatus> statuses);

    /**
     * Provider profile with all required relations loaded.
     *
     * Prevents LazyInitializationException when ProviderService
     * reads User, ServiceArea and ServiceCategory information.
     */
    @Query("""
           select distinct p
           from Provider p
           left join fetch p.user
           left join fetch p.serviceAreas
           left join fetch p.serviceCategories
           where p.user.id = :userId
           """)
    Optional<Provider> findByUserIdWithDetails(
            @Param("userId") Long userId
    );

    /**
     * Candidate pool for matching: approved, verified, active
     * service area + category match.
     *
     * Wallet/subscription eligibility is applied in MatchingService.
     */
    @Query("""
           select distinct p
           from Provider p
           join p.serviceAreas sa
           join p.serviceCategories sc
           where p.status =
                 com.packersmovers.marketplace.common.enums.ProviderStatus.APPROVED
             and sa.id = :serviceAreaId
             and sc.id = :serviceCategoryId
           order by p.priorityScore desc, p.rating desc
           """)
    List<Provider> findEligibleProviders(
            @Param("serviceAreaId") Long serviceAreaId,
            @Param("serviceCategoryId") Long serviceCategoryId
    );
}