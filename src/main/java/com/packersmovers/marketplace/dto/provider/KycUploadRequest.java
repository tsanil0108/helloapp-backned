package com.packersmovers.marketplace.dto.provider;

import com.packersmovers.marketplace.common.enums.KycDocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycUploadRequest {
    @NotNull
    private KycDocType docType;

    @NotBlank
    private String docUrl; // URL of already-uploaded file (object storage handled outside this scaffold)
}
