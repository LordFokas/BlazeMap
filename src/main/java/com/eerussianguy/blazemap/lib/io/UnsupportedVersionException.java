package com.eerussianguy.blazemap.lib.io;

public class UnsupportedVersionException extends UnsupportedOperationException {

    public static UnsupportedVersionException missing(byte version) {
        return new UnsupportedVersionException(Fault.MISSING, "Version " + version + " not supported");
    }

    public static UnsupportedVersionException ahead(byte version, byte current) {
        return new UnsupportedVersionException(Fault.AHEAD, "Version "+version+" is ahead of current: "+current);
    }

    public static UnsupportedVersionException legacy() {
        return new UnsupportedVersionException(Fault.NO_LEGACY, "Legacy reader not defined");
    }

    public enum Fault {
        AHEAD, MISSING, NO_LEGACY
    }

    public final Fault fault;

    private UnsupportedVersionException(Fault fault, String message) {
        super(message);
        this.fault = fault;
    }
}