package com.eerussianguy.blazemap.engine.async;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncChainDelay<I, O> extends AsyncChainItem<I, O> {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private final int delay_ms;

    AsyncChainDelay(AsyncChainItem<?, ?> parent, AsyncChainRoot initiator, int delay_ms) {
        super(parent, initiator);
        if(delay_ms < 0){
            throw new IllegalArgumentException("delay_ms must not be negative.");
        }
        this.delay_ms = delay_ms;
    }

    @Override
    protected void execute(I input) {
        if(delay_ms == 0) {
            nextItem.execute(null);
        }
        else {
            SCHEDULER.schedule(() -> nextItem.execute(null), delay_ms, TimeUnit.MILLISECONDS);
        }
    }
}