package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.quote.QuoteRequest;
import com.packersmovers.marketplace.dto.quote.QuoteResponse;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.QuoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Structured quotes (packing/loading/transport/unloading/other) - section 11 of the architecture doc. */
@RestController
@RequestMapping("/api/provider/quotes")
@RequiredArgsConstructor
@Tag(name = "Provider Quotes", description = "Build and send a structured quote for an unlocked lead")
public class ProviderQuoteController {

    private final QuoteService quoteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuoteResponse> createQuote(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                   @Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success("Quote sent to customer", quoteService.createQuote(principal.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<QuoteResponse>> myQuotes(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(quoteService.getMyQuotes(principal.getUserId()));
    }
}
