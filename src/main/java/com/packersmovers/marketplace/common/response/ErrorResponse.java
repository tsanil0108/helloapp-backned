package com.packersmovers.marketplace.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success = false;
    private String message;
    private int status;
    private String path;
    private Instant timestamp;
    private List<String> details;

    public ErrorResponse(String message, int status, String path, List<String> details) {
        this.success = false;
        this.message = message;
        this.status = status;
        this.path = path;
        this.details = details;
        this.timestamp = Instant.now();
    }
}
