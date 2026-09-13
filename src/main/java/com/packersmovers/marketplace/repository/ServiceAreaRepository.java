package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.ServiceArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, Long> {

    List<ServiceArea> findByActiveTrue();

    @Query("""
        SELECT s
        FROM ServiceArea s
        WHERE LOWER(s.cityOrRegion) = LOWER(:location)
    """)
    Optional<ServiceArea> findFirstByLocationIgnoreCase(
            @Param("location") String location
    );
}