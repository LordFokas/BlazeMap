package com.eerussianguy.blazemap.engine;

import com.eerussianguy.blazemap.engine.async.AsyncChainRoot;
import com.eerussianguy.blazemap.engine.async.AsyncDataCruncher;
import com.eerussianguy.blazemap.engine.async.DebouncingThread;
import com.eerussianguy.blazemap.engine.server.BlazeMapServerEngine;
import com.eerussianguy.blazemap.util.Helpers;

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
        cruncher = new AsyncDataCruncher("Blaze Map");
        serverChain = new AsyncChainRoot(cruncher, BlazeMapServerEngine::submit);
        clientChain = new AsyncChainRoot(cruncher, Helpers::runOnMainThread);
        debouncer = new DebouncingThread("Blaze Map");
    }
}
