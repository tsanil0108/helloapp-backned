package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.auth.CreateAdminRequest;
import com.packersmovers.marketplace.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Super Admin has full control, including creating operational Admin accounts with scoped module permissions. */
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Super-Admin-only operations, starting with Admin account creation")
public class SuperAdminController {

    private final AuthService authService;

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        authService.createAdmin(request);
        return ApiResponse.success("Admin account created", null);
    }
}
