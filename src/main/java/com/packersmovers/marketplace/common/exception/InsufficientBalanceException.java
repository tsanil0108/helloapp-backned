package com.packersmovers.marketplace.common.exception;

/** Thrown when a provider's wallet balance is insufficient for a lead unlock. */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
