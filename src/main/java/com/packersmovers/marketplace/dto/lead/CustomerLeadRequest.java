package com.packersmovers.marketplace.dto.lead;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Submitted directly from the public, no-login customer form (Google/Meta/Website landing pages). */
@Getter
@Setter
public class CustomerLeadRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String mobile;

    @Email
    private String email; // optional

    @NotBlank
    private String pickupLocation;

    @NotBlank
    private String dropLocation;

    @FutureOrPresent(message = "Move date cannot be in the past")
    private LocalDate moveDate;

    private String propertyType;

    @NotBlank
    private String serviceCategory; // matched by name, e.g. "Local Shifting"

    private String inventoryNotes;

    @AssertTrue(message = "Consent is required to proceed")
    private boolean consentGiven;

    // Attribution - captured automatically by the landing page / ad click
    private String source;      // GOOGLE_ADS / META_ADS / WEBSITE / ORGANIC / YOUTUBE / REFERRAL / OTHER
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;

    // Anti-bot
    private String captchaToken;
}
