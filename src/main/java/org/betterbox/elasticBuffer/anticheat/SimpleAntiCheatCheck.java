package org.betterbox.elasticBuffer.anticheat;

import java.util.Collections;
import java.util.Map;

/**
 * A simple immutable check definition that can be registered dynamically.
 */
public class SimpleAntiCheatCheck implements AntiCheatCheck {
    private final String name;
    private final CheckType type;
    private final double defaultSeverity;
    private final Map<String, Object> parameters;

    public SimpleAntiCheatCheck(String name, CheckType type, double defaultSeverity) {
        this(name, type, defaultSeverity, Collections.emptyMap());
    }

    public SimpleAntiCheatCheck(String name, CheckType type, double defaultSeverity, Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.defaultSeverity = defaultSeverity;
        this.parameters = parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(parameters);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CheckType getType() {
        return type;
    }

    @Override
    public double getDefaultSeverity() {
        return defaultSeverity;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }
}
