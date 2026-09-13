package com.packersmovers.marketplace.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SettingResponse {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
}
