package com.packersmovers.marketplace.dto.quote;

import com.packersmovers.marketplace.common.enums.QuoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuoteResponse {
    private Long id;
    private String quoteCode;
    private String leadCode;
    private String providerCompanyName;
    private List<ItemDto> items;
    private BigDecimal totalAmount;
    private LocalDate validUntil;
    private String notes;
    private QuoteStatus status;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemDto {
        private String label;
        private BigDecimal amount;
        private String remarks;
    }
}
