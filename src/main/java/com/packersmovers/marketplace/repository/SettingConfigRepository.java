package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.SettingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingConfigRepository extends JpaRepository<SettingConfig, Long> {
    Optional<SettingConfig> findBySettingKey(String settingKey);
}
