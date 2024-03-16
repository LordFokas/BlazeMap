package com.eerussianguy.blazemap.engine.async;

import java.util.ArrayList;
import java.util.List;

import com.eerussianguy.blazemap.BlazeMap;

public class DebouncingThread {
    private final Thread thread;
    private final List<DebouncingDomain<?>> domains;
    private long nextTaskTimestamp = Long.MAX_VALUE;

    public DebouncingThread(String name) {
        this.domains = new ArrayList<>();
        this.thread = new Thread(this::loop, name + " Debouncer Thread");
        thread.setDaemon(true);
        thread.start();
        BlazeMap.LOGGER.info("Starting {} Debouncer Thread", name);
    }

    /** Not public because this is meant to be called by DebouncingDomain only. */
    void add(DebouncingDomain<?> domain) {
        synchronized(domains) {
            if(!domains.contains(domain)) {
                domains.add(domain);
            }
        }
    }

    /** Called by Cartography. Needed despite "unused", will be dealt with in BME-10 */
    public void remove(DebouncingDomain<?> domain) {
        synchronized(domains) {
            domains.remove(domain);
        }
    }

    /**
     * Called by DebouncingDomain to update us on the timestamp of its next task.
     * If we don't have any tasks before that (lower timestamp) the thread is interrupted to recalculate the wait.
     */
    public void nextTask(long timestamp){
        if(timestamp < nextTaskTimestamp){
            thread.interrupt();
        }
    }

    /** Execute pending tasks across all domains, then sleep until next pending task timestamp. */
    private void work() throws InterruptedException {
        long now = System.currentTimeMillis();
        long nextTaskTimestamp = Long.MAX_VALUE; // When to execute next task. Start at max and find the lowest value (timestamp)

        synchronized(domains) {
            for(DebouncingDomain<?> domain : domains) {
                long nextDomainTask = domain.executePendingTasks(now); // Process all due tasks and get timestamp of next due task.
                if(nextDomainTask < nextTaskTimestamp) nextTaskTimestamp = nextDomainTask; // find the lowest next task timestamp of all domains.
            }
        }

        // A couple ms may have elapsed at this usage of "now" but we don't care.
        // Tasks are not time critical and can wait.
        long wait = nextTaskTimestamp - now;
        this.nextTaskTimestamp = nextTaskTimestamp;
        if(wait == 0) return;
        if(wait < 0) {
            BlazeMap.LOGGER.error("DebouncingThread: Attempted to wait for {}ms", wait);
            wait = 100;
        };

        synchronized(thread) {
            thread.wait(wait);
        }
    }

    /**
     * Main work loop.
     * Infinitely runs work() while tolerating as many faults as possible.
     */
    private void loop() {
        while(true) {
            try{
                work();
            }
            catch(InterruptedException ignored) {}
            catch(Exception e){
                BlazeMap.LOGGER.error("Exception in DebouncingThread main loop!");
                e.printStackTrace();
            }
            catch(Throwable t){
                BlazeMap.LOGGER.error("Throwable in DebouncingThread main loop! ABORTING.");
                t.printStackTrace();
                return;
            }
        }
    }
}
