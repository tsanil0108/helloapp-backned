package com.packersmovers.marketplace.dto.quote;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuoteItemRequest {
    @NotBlank
    private String label; // Packing / Loading / Transport / Unloading / Other

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    private String remarks;
}
