package com.packersmovers.marketplace.dto.provider;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderApprovalRequest {

    public enum Action { APPROVE, REJECT, REQUEST_CHANGES, SUSPEND, REACTIVATE }

    @NotNull
    private Action action;

    private String reason;
}
