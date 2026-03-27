package com.eerussianguy.blazemap.engine;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.engine.server.ServerEngine;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.async.AsyncChainRoot;
import com.eerussianguy.blazemap.lib.async.AsyncDataCruncher;
import com.eerussianguy.blazemap.lib.async.DebouncingThread;

public class BlazeMapAsync {
    private static BlazeMapAsync instance;

    public final DebouncingThread debouncer;
    public final AsyncChainRoot clientChain, serverChain;
    public final AsyncDataCruncher cruncher;

    public static BlazeMapAsync instance() {
        if(instance == null) {
            instance = new BlazeMapAsync();
        }
        return instance;
    }

    private BlazeMapAsync() {
        cruncher = new AsyncDataCruncher("Blaze Map", BlazeMap.LOGGER);
        serverChain = new AsyncChainRoot(cruncher, ServerEngine::submit);
        clientChain = new AsyncChainRoot(cruncher, Helpers::runOnMainThread);
        debouncer = new DebouncingThread("Blaze Map", BlazeMap.LOGGER);
    }
}
