package com.packersmovers.marketplace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserPrincipal principal) {
        return generateToken(principal, jwtProperties.accessTokenExpiryMs(), "access");
    }

    public String generateRefreshToken(CustomUserPrincipal principal) {
        return generateToken(principal, jwtProperties.refreshTokenExpiryMs(), "refresh");
    }

    private String generateToken(CustomUserPrincipal principal, long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(principal.getUserId().toString())
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Date now = new Date();
            return jwtProperties.issuer().equals(claims.getIssuer())
                    && claims.getSubject() != null
                    && claims.getExpiration() != null
                    && claims.getIssuedAt() != null
                    && !claims.getIssuedAt().after(now)
                    && !claims.getExpiration().before(now);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return isValid(token) && "refresh".equals(parseClaims(token).get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }
}
