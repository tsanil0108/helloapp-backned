package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    List<AdminPermission> findByAdminUserId(Long adminUserId);
}
