package com.packersmovers.marketplace.dto.lead;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProviderLeadStatsResponse {

    private long assigned;

    private long viewed;

    private long unlocked;

    private long contacted;

    private long quoteSent;

    private long negotiation;

    private long booked;

    private long serviceInProgress;

    private long completed;

    private long lost;

    private long expired;

    private long cancelled;
}