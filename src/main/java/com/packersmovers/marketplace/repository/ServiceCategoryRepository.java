package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {
    List<ServiceCategory> findByActiveTrue();
    Optional<ServiceCategory> findByNameIgnoreCase(String name);
}
