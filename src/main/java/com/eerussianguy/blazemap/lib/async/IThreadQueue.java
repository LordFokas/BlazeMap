package com.eerussianguy.blazemap.lib.async;

@FunctionalInterface
public interface IThreadQueue {
    void submit(Runnable r);
}

