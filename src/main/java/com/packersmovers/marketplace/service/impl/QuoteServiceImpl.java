package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.enums.QuoteStatus;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.common.exception.UnauthorizedActionException;
import com.packersmovers.marketplace.common.util.LeadIdGenerator;
import com.packersmovers.marketplace.dto.quote.QuoteItemRequest;
import com.packersmovers.marketplace.dto.quote.QuoteRequest;
import com.packersmovers.marketplace.dto.quote.QuoteResponse;
import com.packersmovers.marketplace.entity.Lead;
import com.packersmovers.marketplace.entity.LeadAssignment;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.entity.Quote;
import com.packersmovers.marketplace.entity.QuoteItem;
import com.packersmovers.marketplace.repository.LeadAssignmentRepository;
import com.packersmovers.marketplace.repository.LeadRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.repository.QuoteRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured quotes (packing / loading / transport / unloading / other) built by a provider
 * against a lead they have already paid to unlock - see section 11 of the architecture doc.
 */
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadRepository leadRepository;
    private final ProviderRepository providerRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public QuoteResponse createQuote(Long userId, QuoteRequest request) {
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider profile for this account"));

        LeadAssignment assignment = leadAssignmentRepository.findById(request.getLeadAssignmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("LeadAssignment", request.getLeadAssignmentId()));

        if (!assignment.getProvider().getId().equals(provider.getId())) {
            throw new UnauthorizedActionException("This lead was not offered to your account");
        }
        if (assignment.getStatus() != AssignmentStatus.UNLOCKED) {
            throw new BadRequestException("Unlock this lead before sending a quote");
        }

        Lead lead = assignment.getLead();

        Quote quote = Quote.builder()
                .lead(lead)
                .leadAssignment(assignment)
                .provider(provider)
                .validUntil(request.getValidUntil())
                .notes(request.getNotes())
                .status(QuoteStatus.SENT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<QuoteItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItemRequest itemRequest : request.getItems()) {
            QuoteItem item = QuoteItem.builder()
                    .quote(quote)
                    .label(itemRequest.getLabel())
                    .amount(itemRequest.getAmount())
                    .remarks(itemRequest.getRemarks())
                    .build();
            items.add(item);
            total = total.add(itemRequest.getAmount());
        }
        quote.setItems(items);
        quote.setTotalAmount(total);

        // First save assigns the DB id needed to build a human-friendly quote code (same pattern as Lead.leadCode).
        quote = quoteRepository.save(quote);
        quote.setQuoteCode(LeadIdGenerator.quoteCode(quote.getId()));
        quote = quoteRepository.save(quote);

        if (!lead.getStatus().isTerminal()) {
            lead.setStatus(LeadStatus.QUOTE_SENT);
            leadRepository.save(lead);
        }

        auditService.log("QUOTE_SENT", "Quote", quote.getId(), null, quote.getQuoteCode(),
                "lead=" + lead.getLeadCode() + " total=" + total);

        return toResponse(quote);
    }

    @Override
    public List<QuoteResponse> getQuotesForLead(Long leadId) {
        return quoteRepository.findByLeadId(leadId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<QuoteResponse> getMyQuotes(Long userId) {
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No provider profile for this account"));
        return quoteRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private QuoteResponse toResponse(Quote quote) {
        List<QuoteResponse.ItemDto> items = quote.getItems().stream()
                .map(i -> QuoteResponse.ItemDto.builder()
                        .label(i.getLabel())
                        .amount(i.getAmount())
                        .remarks(i.getRemarks())
                        .build())
                .toList();

        return QuoteResponse.builder()
                .id(quote.getId())
                .quoteCode(quote.getQuoteCode())
                .leadCode(quote.getLead().getLeadCode())
                .providerCompanyName(quote.getProvider().getCompanyName())
                .items(items)
                .totalAmount(quote.getTotalAmount())
                .validUntil(quote.getValidUntil())
                .notes(quote.getNotes())
                .status(quote.getStatus())
                .build();
    }
}
