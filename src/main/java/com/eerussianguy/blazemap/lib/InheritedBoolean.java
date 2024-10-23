package com.eerussianguy.blazemap.lib;

import java.util.function.BooleanSupplier;

public enum InheritedBoolean {
    TRUE, FALSE, INHERITED;

    public static InheritedBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean getOrInherit(BooleanSupplier parent) {
        return switch(this) {
            case TRUE -> true;
            case FALSE -> false;
            case INHERITED -> parent.getAsBoolean();
        };
    }

    public boolean getOrThrow() {
        return switch(this) {
            case TRUE -> true;
            case FALSE -> false;
            case INHERITED -> throw new IllegalStateException("INHERITED has no direct value");
        };
    }

    public boolean isDirect() {
        return this != INHERITED;
    }
}
