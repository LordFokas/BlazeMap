package com.eerussianguy.blazemap.lib.async;

import java.util.function.Function;

public class AsyncChainRoot {
    final IThreadQueue gameThreadQueue;
    final IThreadQueue dataThreadQueue;

    public AsyncChainRoot(AsyncDataCruncher asyncDataCruncher, IThreadQueue gameThreadQueue) {
        this.dataThreadQueue = asyncDataCruncher::submit;
        this.gameThreadQueue = gameThreadQueue;
    }

    public <O> AsyncChainItem<Void, O> startOnGameThread(Function<Void, O> task) {
        return new AsyncChainTask<>(null, task, gameThreadQueue, this);
    }

    public <O> AsyncChainItem<Void, O> startOnDataThread(Function<Void, O> task) {
        return new AsyncChainTask<>(null, task, dataThreadQueue, this);
    }

    public AsyncChainItem<Void, Void> startWithDelay(int delay_ms) {
        return new AsyncChainDelay<>(null, this, delay_ms);
    }

    public AsyncChainItem<Void, Void> begin() {
        return new AsyncChainDummyItem(this);
    }

    public void runOnGameThread(Runnable r) {
        gameThreadQueue.submit(r);
    }

    public void runOnDataThread(Runnable r) {
        dataThreadQueue.submit(r);
    }



    private static class AsyncChainDummyItem extends AsyncChainItem<Void, Void> {
        AsyncChainDummyItem(AsyncChainRoot initiator) {
            super(null, initiator);
        }

        @Override
        protected void executeTask(Void input) {
            this.nextItem.executeTask(null);
        }
    }
}
