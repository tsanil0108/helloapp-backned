package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenSession r where r.tokenHash = :tokenHash")
    Optional<RefreshTokenSession> findForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshTokenSession> findByUserIdAndRevokedAtIsNull(Long userId);
}
