package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.quote.QuoteRequest;
import com.packersmovers.marketplace.dto.quote.QuoteResponse;

import java.util.List;

public interface QuoteService {

    /** Provider builds and sends a structured quote for a lead they have already unlocked. */
    QuoteResponse createQuote(Long userId, QuoteRequest request);

    /** All quotes a customer/admin can compare for a given lead. */
    List<QuoteResponse> getQuotesForLead(Long leadId);

    /** A provider's own quote history. */
    List<QuoteResponse> getMyQuotes(Long userId);
}
