package com.packersmovers.marketplace.dto.auth;

import com.packersmovers.marketplace.common.enums.AdminModule;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateAdminRequest {

    @NotBlank
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String mobile;

    @NotBlank
    private String password;

    @NotEmpty(message = "Grant at least one module permission")
    private List<ModulePermissionDto> permissions;

    @Getter
    @Setter
    public static class ModulePermissionDto {
        private AdminModule module;
        private boolean canView;
        private boolean canEdit;
        private boolean canDelete;
    }
}
