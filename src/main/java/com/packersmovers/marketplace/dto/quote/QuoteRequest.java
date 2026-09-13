package com.packersmovers.marketplace.dto.quote;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class QuoteRequest {
    @NotNull
    private Long leadAssignmentId;

    @NotEmpty(message = "Add at least one quote line item")
    @Valid
    private List<QuoteItemRequest> items;

    private LocalDate validUntil;
    private String notes;
}
