package com.packersmovers.marketplace.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingUpdateRequest {
    @NotBlank
    private String settingKey;
    @NotBlank
    private String settingValue;
    private String description;
}
