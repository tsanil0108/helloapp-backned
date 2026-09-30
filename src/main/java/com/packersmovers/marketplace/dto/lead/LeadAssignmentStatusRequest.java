package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadAssignmentStatusRequest {

    @NotNull
    private AssignmentStatus status;

    private String contactMethod;

    @Size(max = 100)
    private String lostReason;

    private String completionNotes;
}