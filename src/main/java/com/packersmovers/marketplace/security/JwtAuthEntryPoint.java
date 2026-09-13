package com.packersmovers.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.packersmovers.marketplace.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a clean JSON 401 instead of the default Spring Security HTML/basic-auth challenge. */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse("Authentication required. Please log in.",
                HttpStatus.UNAUTHORIZED.value(), request.getRequestURI(), null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
