package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.AdminModule;
import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fine-grained module permission for an ADMIN user (SUPER_ADMIN implicitly has everything).
 * RBAC = Role (User.role) -> module permission -> action, all changes audited via AuditLog.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "admin_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"admin_user_id", "module"}))
public class AdminPermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(nullable = false, length = 30)
    private AdminModule module;

    @Builder.Default
    private boolean canView = true;
    @Builder.Default
    private boolean canEdit = false;
    @Builder.Default
    private boolean canDelete = false;
}
