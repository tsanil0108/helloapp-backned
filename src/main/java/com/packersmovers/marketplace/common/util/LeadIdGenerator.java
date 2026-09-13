package com.packersmovers.marketplace.common.util;

/** Small helper for human-readable codes; entities set the real code after their DB id is known. */
public final class LeadIdGenerator {
    private LeadIdGenerator() {
    }

    public static String leadCode(Long id) {
        return "L-" + (1000 + id);
    }

    public static String quoteCode(Long id) {
        return "Q-" + (5000 + id);
    }
}
