package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.RoleName;
import com.packersmovers.marketplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByMobile(String mobile);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobile(String mobile);
    List<User> findByRole(RoleName role);
}
