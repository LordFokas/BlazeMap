package com.eerussianguy.blazemap.lib.async;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

public final class AsyncDataCruncher {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;
    private final Object mutex = new Object();
    private final LinkedList<Thread> threads = new LinkedList<>();

    public AsyncDataCruncher(String name, Logger logger) {
        int cores = Runtime.getRuntime().availableProcessors();
        logger.info("Starting {} {} AsyncDataCruncher Threads", cores, name);
        for(int i = 0; i < cores; i++) {
            Thread thread = new Thread(this::loop);
            thread.setName(name + " AsyncDataCruncher #" + i);
            thread.setDaemon(true);
            thread.setPriority(7);
            thread.start();
            threads.add(thread);
            logger.info("Started {}", thread.getName());
        }
        logger.info("Started {} {} AsyncDataCruncher Threads", cores, name);
    }

    public int poolSize() {
        return threads.size();
    }

    public void assertIsOnDataCruncherThread() {
        if(!threads.contains(Thread.currentThread())) {
            throw new IllegalStateException("Operation can only be performed in the AsyncDataCruncher thread");
        }
    }

    public IThreadAsserter getThreadAsserter() {
        return this::assertIsOnDataCruncherThread;
    }

    public void stop() {
        running = false;
    }

    public void submit(Runnable r) {
        tasks.add(r);
        synchronized(mutex) {
            mutex.notify();
        }
    }

    private void loop() {
        while(running) {
            this.work();
            try {
                synchronized(mutex) {
                    mutex.wait();
                }
            }
            catch(InterruptedException ex) {
                // We can't tolerate interrupts on the worker threads as that messes up with java.nio
                // The runtime exception helps us realize if it is happening
                throw new RuntimeException(ex);
            }
        }
    }

    private void work() {
        while(!tasks.isEmpty()) {
            Runnable task = tasks.poll();
            if(task == null) continue;
            try {task.run();}
            catch(Throwable t) {t.printStackTrace();}
        }
    }

    @FunctionalInterface
    public interface IThreadAsserter {
        void assertCurrentThread();
    }
}
