package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findFirstByMobileOrderByCreatedAtDesc(String mobile);
    List<Customer> findByMobile(String mobile);
}
