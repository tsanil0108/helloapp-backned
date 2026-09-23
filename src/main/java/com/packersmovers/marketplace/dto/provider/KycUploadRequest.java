package com.packersmovers.marketplace.dto.provider;

import com.packersmovers.marketplace.common.enums.KycDocType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycUploadRequest {

    @NotNull
    private KycDocType docType;
}