package com.eerussianguy.blazemap.api.event;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.util.StorageAccess;

/**
 * Server Engine lifecycle events.
 *
 * @author LordFokas
 */
public abstract class ServerEngineEvent extends Event {
    /** The file storage where Blaze Map stores all the data for this server */
    public final StorageAccess.ServerStorage serverStorage;

    protected ServerEngineEvent(StorageAccess.ServerStorage storage) {
        this.serverStorage = storage;
    }

    /** Fired by the Blaze Map engine, after the server engine starts. */
    public static class EngineStartingEvent extends ServerEngineEvent {
        public EngineStartingEvent(StorageAccess.ServerStorage storage) {
            super(storage);
        }
    }

    /** Fired by the Blaze Map engine, before the server engine stops. */
    public static class EngineStoppingEvent extends ServerEngineEvent {
        public EngineStoppingEvent(StorageAccess.ServerStorage storage) {
            super(storage);
        }
    }
}
