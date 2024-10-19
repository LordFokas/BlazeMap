package com.eerussianguy.blazemap.feature.waypoints.service;

import java.util.function.BooleanSupplier;

public enum DefaultedBoolean {
    TRUE, FALSE, DEFAULT;

    public boolean getOrDefault(BooleanSupplier parent) {
        return switch(this) {
            case TRUE -> true;
            case FALSE -> false;
            case DEFAULT -> parent.getAsBoolean();
        };
    }

    public boolean getOrThrow() {
        return switch(this) {
            case TRUE -> true;
            case FALSE -> false;
            case DEFAULT -> throw new IllegalStateException("DEFAULT has no direct value");
        };
    }

    public boolean isDirect() {
        return this != DEFAULT;
    }
}
