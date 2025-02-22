package com.eerussianguy.blazemap.lib;

import java.util.function.BooleanSupplier;

public enum InheritedBoolean {
    TRUE, FALSE, DEFAULT;

    public static InheritedBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean getOrInherit(BooleanSupplier parent) {
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
