package com.eerussianguy.blazemap.engine.async;

import java.util.Objects;
import java.util.function.Function;

public abstract class AsyncChainItem<I, O> {
    private final AsyncChainRoot initiator;
    private final AsyncChainItem<?, ?> root;
    protected AsyncChainItem<O, ?> nextItem;
    private boolean closed = false;

    AsyncChainItem(AsyncChainItem<?, ?> parent, AsyncChainRoot initiator) {
        if(parent == null){
            this.root = null;
        } else {
            this.root = Objects.requireNonNullElse(parent.root, parent);
        }
        this.initiator = initiator;
    }

    public <N> AsyncChainItem<O, N> thenOnGameThread(Function<O, N> task) {
        return thenOnThread(task, initiator.gameThreadQueue);
    }

    public <N> AsyncChainItem<O, N> thenOnDataThread(Function<O, N> task) {
        return thenOnThread(task, initiator.dataThreadQueue);
    }

    private <N> AsyncChainItem<O, N> thenOnThread(Function<O, N> task, IThreadQueue threadQueue) {
        this.closeItem();

        AsyncChainItem<O, N> next = new AsyncChainTask<>(this, task, threadQueue, initiator);
        this.nextItem = next;
        return next;
    }

    // This function causes the game to crash with NoClassDefFoundError for some unknown reason. No idea why. - Respectable Username
    // Whatever was causing this may have been changed with the refactor. Needs reevaluation. - LordFokas
    public AsyncChainItem<O, O> thenDelay(int delay_ms) {
        this.closeItem();

        AsyncChainItem<O, O> next = new AsyncChainDelay<>(this, initiator, delay_ms);
        this.nextItem = next;
        return next;
    }

    protected void closeItem(){
        if(closed) throw new IllegalStateException("AsyncChain is already closed");
        closed = true;
    }

    public void start() {
        (this.root == null ? this : this.root).execute(null);
    }

    protected abstract void execute(I input);
}