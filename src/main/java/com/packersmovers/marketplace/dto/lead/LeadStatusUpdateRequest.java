package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadStatusUpdateRequest {
    @NotNull
    private LeadStatus status;
    private String notes;
}
