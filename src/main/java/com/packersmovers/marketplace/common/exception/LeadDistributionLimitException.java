package com.packersmovers.marketplace.common.exception;

/** Thrown when a lead has already reached its maximum provider distribution count. */
public class LeadDistributionLimitException extends RuntimeException {
    public LeadDistributionLimitException(String message) {
        super(message);
    }
}
