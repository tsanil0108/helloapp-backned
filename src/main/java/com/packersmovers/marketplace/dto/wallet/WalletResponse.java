package com.packersmovers.marketplace.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class WalletResponse {
    private Long providerId;
    private BigDecimal balance;
}
