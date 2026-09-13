package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.NotificationChannel;
import com.packersmovers.marketplace.common.enums.ProviderStatus;
import com.packersmovers.marketplace.common.enums.RoleName;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.dto.auth.ChangePasswordRequest;
import com.packersmovers.marketplace.dto.auth.CreateAdminRequest;
import com.packersmovers.marketplace.dto.auth.ForgotPasswordRequest;
import com.packersmovers.marketplace.dto.auth.JwtAuthResponse;
import com.packersmovers.marketplace.dto.auth.LoginRequest;
import com.packersmovers.marketplace.dto.auth.RegisterProviderRequest;
import com.packersmovers.marketplace.dto.auth.ResetPasswordRequest;
import com.packersmovers.marketplace.entity.AdminPermission;
import com.packersmovers.marketplace.entity.PasswordResetToken;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.entity.RefreshTokenSession;
import com.packersmovers.marketplace.entity.ServiceArea;
import com.packersmovers.marketplace.entity.ServiceCategory;
import com.packersmovers.marketplace.entity.User;
import com.packersmovers.marketplace.entity.Wallet;
import com.packersmovers.marketplace.repository.AdminPermissionRepository;
import com.packersmovers.marketplace.repository.PasswordResetTokenRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.repository.RefreshTokenSessionRepository;
import com.packersmovers.marketplace.repository.ServiceAreaRepository;
import com.packersmovers.marketplace.repository.ServiceCategoryRepository;
import com.packersmovers.marketplace.repository.UserRepository;
import com.packersmovers.marketplace.repository.WalletRepository;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.security.JwtProperties;
import com.packersmovers.marketplace.security.JwtTokenProvider;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.AuthService;
import com.packersmovers.marketplace.service.NotificationService;
import com.packersmovers.marketplace.service.PasswordResetDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final ProviderRepository providerRepository;

    private final WalletRepository walletRepository;

    private final ServiceAreaRepository serviceAreaRepository;

    private final ServiceCategoryRepository serviceCategoryRepository;

    private final AdminPermissionRepository adminPermissionRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final AuditService auditService;

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    private final JwtProperties jwtProperties;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordResetDeliveryService passwordResetDeliveryService;

    private final NotificationService notificationService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final long RESET_TOKEN_EXPIRY_MINUTES = 30;


    // ============================================================
    // LOGIN
    // ============================================================

    @Override
    @Transactional
    public JwtAuthResponse login(LoginRequest request) {

        if (request == null
                || request.getIdentifier() == null
                || request.getIdentifier().isBlank()) {

            throw new BadRequestException(
                    "Email or mobile number is required"
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Password is required"
            );
        }

        String identifier = request.getIdentifier().trim();

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        identifier,
                        request.getPassword()
                );

        var authentication =
                authenticationManager.authenticate(authToken);

        CustomUserPrincipal principal =
                (CustomUserPrincipal) authentication.getPrincipal();

        auditService.log(
                "LOGIN",
                "User",
                principal.getUserId(),
                null,
                null,
                "Successful login"
        );

        return buildTokens(principal);
    }


    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    @Override
    @Transactional
    public JwtAuthResponse refresh(String refreshToken) {

        if (refreshToken == null
                || refreshToken.isBlank()) {

            throw new BadRequestException(
                    "Refresh token is required"
            );
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {

            throw new BadRequestException(
                    "Invalid or expired refresh token"
            );
        }

        Long userId =
                jwtTokenProvider.getUserId(refreshToken);

        if (userId == null) {

            throw new BadRequestException(
                    "Invalid refresh token"
            );
        }

        String tokenHash = hash(refreshToken);

        RefreshTokenSession session =
                refreshTokenSessionRepository
                        .findForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Refresh session is invalid or revoked"
                                )
                        );

        Instant now = Instant.now();

        if (!session.getUser().getId().equals(userId)) {

            throw new BadRequestException(
                    "Refresh session is invalid"
            );
        }

        if (session.getRevokedAt() != null) {

            throw new BadRequestException(
                    "Refresh token has already been revoked"
            );
        }

        if (!session.getExpiresAt().isAfter(now)) {

            throw new BadRequestException(
                    "Refresh token has expired"
            );
        }

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "User",
                                        userId
                                )
                        );

        if (!user.isActive()) {

            throw new BadRequestException(
                    "User account is inactive"
            );
        }

        /*
         * Rotate old refresh token.
         */
        session.setRevokedAt(now);

        JwtAuthResponse response =
                buildTokens(
                        new CustomUserPrincipal(user)
                );

        session.setReplacedByHash(
                hash(response.getRefreshToken())
        );

        refreshTokenSessionRepository.save(session);

        auditService.log(
                "TOKEN_REFRESH",
                "User",
                userId,
                null,
                null,
                "Refresh token rotated successfully"
        );

        return response;
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    @Override
    @Transactional
    public void logout(String refreshToken) {

        if (refreshToken == null
                || refreshToken.isBlank()) {

            return;
        }

        String tokenHash = hash(refreshToken);

        refreshTokenSessionRepository
                .findForUpdate(tokenHash)
                .ifPresent(session -> {

                    if (session.getRevokedAt() == null) {

                        session.setRevokedAt(
                                Instant.now()
                        );

                        refreshTokenSessionRepository.save(
                                session
                        );
                    }
                });
    }


    // ============================================================
    // LOGOUT ALL
    // ============================================================

    @Override
    @Transactional
    public void logoutAll(Long userId) {

        if (userId == null) {

            throw new BadRequestException(
                    "User ID is required"
            );
        }

        Instant now = Instant.now();

        List<RefreshTokenSession> sessions =
                refreshTokenSessionRepository
                        .findByUserIdAndRevokedAtIsNull(userId);

        /*
         * No lambda here.
         * This avoids effectively-final problems.
         */
        for (RefreshTokenSession session : sessions) {

            session.setRevokedAt(now);

            refreshTokenSessionRepository.save(session);
        }

        auditService.log(
                "LOGOUT_ALL",
                "User",
                userId,
                null,
                null,
                "All refresh sessions revoked"
        );
    }


    // ============================================================
    // FORGOT PASSWORD
    // ============================================================

    @Override
    @Transactional
    public void forgotPassword(
            ForgotPasswordRequest request
    ) {

        if (request == null
                || request.identifier() == null
                || request.identifier().isBlank()) {

            throw new BadRequestException(
                    "Email or mobile number is required"
            );
        }

        String identifier =
                request.identifier().trim();

        User user =
                userRepository
                        .findByEmailIgnoreCase(identifier)
                        .or(() ->
                                userRepository.findByMobile(
                                        identifier
                                )
                        )
                        .orElse(null);

        /*
         * Do not reveal whether account exists.
         */
        if (user == null || !user.isActive()) {

            log.info(
                    "Password reset requested for unknown/inactive identifier"
            );

            return;
        }

        Instant now = Instant.now();

        passwordResetTokenRepository
                .invalidateActiveTokens(
                        user.getId(),
                        now
                );

        /*
         * Generate secure 256-bit reset token.
         */
        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        String rawToken =
                HexFormat.of().formatHex(bytes);

        String tokenHash =
                hash(rawToken);

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiresAt(
                                now.plusSeconds(
                                        RESET_TOKEN_EXPIRY_MINUTES * 60
                                )
                        )
                        .build();

        passwordResetTokenRepository.save(
                resetToken
        );

        /*
         * Raw token is sent through delivery service.
         * Only hash is stored in DB.
         */
        passwordResetDeliveryService.send(
                user,
                rawToken
        );

        auditService.log(
                "PASSWORD_RESET_REQUESTED",
                "User",
                user.getId(),
                null,
                null,
                "Password reset requested"
        );
    }


    // ============================================================
    // RESET PASSWORD
    // ============================================================

    @Override
    @Transactional
    public void resetPassword(
            ResetPasswordRequest request
    ) {

        if (request == null
                || request.token() == null
                || request.token().isBlank()) {

            throw new BadRequestException(
                    "Reset token is required"
            );
        }

        if (request.newPassword() == null
                || request.newPassword().isBlank()) {

            throw new BadRequestException(
                    "New password is required"
            );
        }

        Instant now = Instant.now();

        String tokenHash =
                hash(request.token().trim());

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findForUpdate(tokenHash)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid or expired reset token"
                                )
                        );

        if (resetToken.getUsedAt() != null
                || !resetToken.getExpiresAt().isAfter(now)) {

            throw new BadRequestException(
                    "Invalid or expired reset token"
            );
        }

        User user =
                resetToken.getUser();

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {

            throw new BadRequestException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        user.setLocked(false);

        userRepository.save(user);

        resetToken.setUsedAt(now);

        passwordResetTokenRepository.save(
                resetToken
        );

        /*
         * Revoke all existing refresh sessions.
         */
        List<RefreshTokenSession> sessions =
                refreshTokenSessionRepository
                        .findByUserIdAndRevokedAtIsNull(
                                user.getId()
                        );

        for (RefreshTokenSession session : sessions) {

            session.setRevokedAt(now);

            refreshTokenSessionRepository.save(session);
        }

        auditService.log(
                "PASSWORD_RESET",
                "User",
                user.getId(),
                null,
                null,
                "Password reset completed"
        );
    }


    // ============================================================
    // CHANGE PASSWORD
    // ============================================================

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {

        if (userId == null) {

            throw new BadRequestException(
                    "User ID is required"
            );
        }

        if (request == null) {

            throw new BadRequestException(
                    "Password request is required"
            );
        }

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "User",
                                        userId
                                )
                        );

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {

            throw new BadRequestException(
                    "Current password is incorrect"
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {

            throw new BadRequestException(
                    "New password must be different from the current password"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        userRepository.save(user);

        /*
         * Revoke all refresh sessions.
         */
        Instant now = Instant.now();

        List<RefreshTokenSession> sessions =
                refreshTokenSessionRepository
                        .findByUserIdAndRevokedAtIsNull(userId);

        for (RefreshTokenSession session : sessions) {

            session.setRevokedAt(now);

            refreshTokenSessionRepository.save(session);
        }

        auditService.log(
                "PASSWORD_CHANGED",
                "User",
                userId,
                null,
                null,
                "Password changed; existing refresh sessions revoked"
        );
    }


    // ============================================================
    // PROVIDER REGISTRATION
    // ============================================================

    @Override
    @Transactional
    public JwtAuthResponse registerProvider(
            RegisterProviderRequest request
    ) {

        if (request == null) {

            throw new BadRequestException(
                    "Registration request is required"
            );
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            throw new BadRequestException(
                    "Email is required"
            );
        }

        if (request.getMobile() == null
                || request.getMobile().isBlank()) {

            throw new BadRequestException(
                    "Mobile number is required"
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Password is required"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(
                request.getEmail().trim()
        )) {

            throw new BadRequestException(
                    "An account already exists with this email"
            );
        }

        if (userRepository.existsByMobile(
                request.getMobile().trim()
        )) {

            throw new BadRequestException(
                    "An account already exists with this mobile number"
            );
        }

        User user =
                User.builder()
                        .fullName(
                                request.getOwnerName()
                        )
                        .email(
                                request.getEmail().trim()
                        )
                        .mobile(
                                request.getMobile().trim()
                        )
                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(RoleName.PROVIDER)
                        .active(true)
                        .build();

        user = userRepository.save(user);

        /*
         * IMPORTANT:
         * Create final reference after save.
         * This prevents effectively-final issues if
         * user is used inside a lambda anywhere later.
         */
        final User savedUser = user;

        Set<ServiceArea> areas =
                new HashSet<>(
                        serviceAreaRepository.findAllById(
                                request.getServiceAreaIds()
                        )
                );

        Set<ServiceCategory> categories =
                new HashSet<>(
                        serviceCategoryRepository.findAllById(
                                request.getServiceCategoryIds()
                        )
                );

        if (areas.isEmpty()
                || categories.isEmpty()) {

            throw new BadRequestException(
                    "Selected service areas / categories are invalid"
            );
        }

        Provider provider =
                Provider.builder()
                        .user(savedUser)
                        .companyName(
                                request.getCompanyName()
                        )
                        .ownerName(
                                request.getOwnerName()
                        )
                        .gstNumber(
                                request.getGstNumber()
                        )
                        .panNumber(
                                request.getPanNumber()
                        )
                        .status(
                                ProviderStatus.SUBMITTED
                        )
                        .serviceAreas(areas)
                        .serviceCategories(categories)
                        .build();

        provider =
                providerRepository.save(provider);

        /*
         * Final provider reference.
         */
        final Provider savedProvider = provider;

        /*
         * Create provider wallet with zero balance.
         */
        walletRepository.save(
                Wallet.builder()
                        .provider(savedProvider)
                        .balance(BigDecimal.ZERO)
                        .build()
        );

        auditService.log(
                "PROVIDER_REGISTERED",
                "Provider",
                savedProvider.getId(),
                null,
                ProviderStatus.SUBMITTED.name(),
                "New provider self-registration, pending Super Admin review"
        );

        /*
         * Registration notification.
         */
        notificationService.notify(
                savedUser,
                "Registration received",
                "Your provider registration is under review. "
                        + "You'll be notified once approved.",
                NotificationChannel.IN_APP,
                "PROVIDER:" + savedProvider.getId()
        );

        /*
         * Return JWT tokens for registered provider.
         */
        return buildTokens(
                new CustomUserPrincipal(savedUser)
        );
    }


    // ============================================================
    // CREATE ADMIN
    // ============================================================

    @Override
    @Transactional
    public void createAdmin(
            CreateAdminRequest request
    ) {

        if (request == null) {

            throw new BadRequestException(
                    "Admin request is required"
            );
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {

            throw new BadRequestException(
                    "Admin email is required"
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Admin password is required"
            );
        }

        if (userRepository.existsByEmailIgnoreCase(
                request.getEmail().trim()
        )) {

            throw new BadRequestException(
                    "An account already exists with this email"
            );
        }

        User admin =
                User.builder()
                        .fullName(
                                request.getFullName()
                        )
                        .email(
                                request.getEmail().trim()
                        )
                        .mobile(
                                request.getMobile()
                        )
                        .passwordHash(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(RoleName.ADMIN)
                        .active(true)
                        .build();

        admin = userRepository.save(admin);

        /*
         * THIS IS THE FIX FOR:
         *
         * local variables referenced from a lambda expression
         * must be final or effectively final
         */
        final User savedAdmin = admin;

        if (request.getPermissions() != null
                && !request.getPermissions().isEmpty()) {

            List<AdminPermission> permissions =
                    request.getPermissions()
                            .stream()
                            .map(permission ->
                                    AdminPermission.builder()
                                            .adminUser(savedAdmin)
                                            .module(
                                                    permission.getModule()
                                            )
                                            .canView(
                                                    permission.isCanView()
                                            )
                                            .canEdit(
                                                    permission.isCanEdit()
                                            )
                                            .canDelete(
                                                    permission.isCanDelete()
                                            )
                                            .build()
                            )
                            .toList();

            adminPermissionRepository.saveAll(
                    permissions
            );
        }

        auditService.log(
                "ADMIN_CREATED",
                "User",
                savedAdmin.getId(),
                null,
                "ADMIN",
                "Super Admin created a new Admin account"
        );
    }


    // ============================================================
    // TOKEN BUILDER
    // ============================================================

    private JwtAuthResponse buildTokens(
            CustomUserPrincipal principal
    ) {

        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        principal
                );

        String refreshToken =
                jwtTokenProvider.generateRefreshToken(
                        principal
                );

        User tokenUser =
                userRepository.getReferenceById(
                        principal.getUserId()
                );

        RefreshTokenSession session =
                RefreshTokenSession.builder()
                        .user(tokenUser)
                        .tokenHash(
                                hash(refreshToken)
                        )
                        .expiresAt(
                                Instant.now().plusMillis(
                                        jwtProperties
                                                .refreshTokenExpiryMs()
                                )
                        )
                        .build();

        refreshTokenSessionRepository.save(
                session
        );

        return JwtAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(principal.getUserId())
                .fullName(principal.getFullName())
                .role(principal.getRole())
                .build();
    }


    // ============================================================
    // SHA-256 HASH
    // ============================================================

    private String hash(String token) {

        try {

            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    token.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            );

            StringBuilder output =
                    new StringBuilder(64);

            for (byte value : digest) {

                output.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }

            return output.toString();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to hash security token",
                    exception
            );
        }
    }
}