package com.eerussianguy.blazemap.api.event;

import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.util.StorageAccess;

/**
 * Fired by the Blaze Map engine after the game connects to a new server. <br>
 * The engine is not yet ready to serve mapping related requests, for that see DimensionChangedEvent.
 *
 * @author LordFokas
 */
public class ServerJoinedEvent extends Event {
    /**
     * The internal ID used to represent this server
     */
    public final String serverID;

    /**
     * The file storage where Blaze Map stores all the data for this server
     */
    public final StorageAccess.ServerStorage serverStorage;

    /**
     * If the server we connected to has Blaze Map present
     */
    public final boolean serverHasBlazeMap;

    public ServerJoinedEvent(String serverID, StorageAccess.ServerStorage storage, boolean serverHasBlazeMap) {
        this.serverID = serverID;
        this.serverStorage = storage;
        this.serverHasBlazeMap = serverHasBlazeMap;
    }
}
