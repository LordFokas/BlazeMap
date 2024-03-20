package com.eerussianguy.blazemap.engine.async;

import java.util.function.Function;

public class AsyncChainTask<I, O> extends AsyncChainItem<I, O>{
    private final Function<I, O> task;
    private final IThreadQueue threadQueue;

    AsyncChainTask(AsyncChainItem<?, ?> parent, Function<I, O> task, IThreadQueue threadQueue, AsyncChainRoot initiator) {
        super(parent, initiator);
        this.task = task;
        this.threadQueue = threadQueue;
    }

    @Override
    protected void execute(I input) {
        threadQueue.submit(() -> {
            O output = task.apply(input);

            if(nextItem != null) {
                nextItem.execute(output);
            }
        });
    }
}