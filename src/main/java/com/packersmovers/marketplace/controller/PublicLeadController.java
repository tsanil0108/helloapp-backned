package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.lead.CustomerLeadRequest;
import com.packersmovers.marketplace.dto.lead.LeadResponse;
import com.packersmovers.marketplace.service.LeadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * No login, no OTP, no app download - this is the entire public surface a moving customer touches
 * (landing page form -> lead creation, plus a lightweight "My Requests" lookup by mobile number).
 */
@RestController
@RequestMapping("/api/public/leads")
@RequiredArgsConstructor
@Tag(name = "Public Leads", description = "Frictionless customer-facing lead capture - no authentication")
public class PublicLeadController {

    private final LeadService leadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeadResponse> createLead(@Valid @RequestBody CustomerLeadRequest request,
                                                 HttpServletRequest httpRequest) {
        return ApiResponse.success("Thanks! We're matching you with verified movers now.",
                leadService.createLead(request, resolveClientIp(httpRequest)));
    }

    @GetMapping("/{leadCode}")
    public ApiResponse<LeadResponse> getByCode(@PathVariable String leadCode) {
        return ApiResponse.success(leadService.getByCode(leadCode));
    }

    /** "My Requests" - active/past status lookup by the mobile number used at submission time. */
    @GetMapping("/my-requests")
    public ApiResponse<List<LeadResponse>> myRequests(@RequestParam String mobile) {
        return ApiResponse.success(leadService.myRequests(mobile));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
