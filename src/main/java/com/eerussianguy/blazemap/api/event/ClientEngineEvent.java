package com.eerussianguy.blazemap.api.event;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.util.StorageAccess;

/**
 * Client Engine lifecycle events.
 *
 * @author LordFokas
 */
public abstract class ClientEngineEvent extends Event {
    /**
     * The internal ID used to represent this server
     */
    public final String serverID;

    /**
     * The file storage where Blaze Map stores all the data for this server
     */
    public final StorageAccess.ServerStorage serverStorage;

    /**
     * If the server has Blaze Map present
     */
    public final boolean serverHasBlazeMap;

    protected ClientEngineEvent(String serverID, StorageAccess.ServerStorage storage, boolean serverHasBlazeMap) {
        this.serverID = serverID;
        this.serverStorage = storage;
        this.serverHasBlazeMap = serverHasBlazeMap;
    }

    /** Fired by the Blaze Map engine after the game connects to a new server, after the engine starts. */
    public static class EngineStartingEvent extends ClientEngineEvent {
        public EngineStartingEvent(String serverID, StorageAccess.ServerStorage storage, boolean serverHasBlazeMap) {
            super(serverID, storage, serverHasBlazeMap);
        }
    }

    /** Fired by the Blaze Map engine before the game disconnects from a server, before the engine stops. */
    public static class EngineStoppingEvent extends ClientEngineEvent {
        public EngineStoppingEvent(String serverID, StorageAccess.ServerStorage storage, boolean serverHasBlazeMap) {
            super(serverID, storage, serverHasBlazeMap);
        }
    }
}
