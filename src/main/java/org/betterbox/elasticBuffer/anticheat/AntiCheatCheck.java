package org.betterbox.elasticBuffer.anticheat;

import java.util.Map;

/**
 * Contract for anti-cheat checks to allow easy extension without touching core logic.
 */
public interface AntiCheatCheck {
    String getName();

    CheckType getType();

    /**
     * Returns a severity value for the violation produced by this check.
     */
    double getDefaultSeverity();

    /**
     * Optional parameters for the check (e.g., reach distance, cps limit).
     */
    Map<String, Object> getParameters();
}
