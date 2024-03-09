package com.eerussianguy.blazemap.engine.async;

import java.util.ArrayList;
import java.util.List;

import com.eerussianguy.blazemap.BlazeMap;

public class DebouncingThread {
    private final Thread thread;
    private final List<DebouncingDomain<?>> domains;

    public DebouncingThread(String name) {
        this.domains = new ArrayList<>();
        this.thread = new Thread(this::loop, name + " Debouncer Thread");
        thread.setDaemon(true);
        thread.start();
        BlazeMap.LOGGER.info("Starting {} Debouncer Thread", name);
    }

    void add(DebouncingDomain<?> domain) {
        synchronized(domains) {
            if(!domains.contains(domain)) {
                domains.add(domain);
                domain.setThread(thread);
                thread.interrupt();
            }
        }
    }

    public void remove(DebouncingDomain<?> domain) {
        synchronized(domains) {
            domains.remove(domain);
        }
    }

    private void work() throws InterruptedException {
        long next = Long.MAX_VALUE; // When to execute next task. Start at max and find the lowest value (timestamp)

        synchronized(domains) {
            for(DebouncingDomain<?> domain : domains) {
                long wait = domain.pop(); // Process all due tasks and get timestamp of next due task.
                if(wait < next) next = wait; // find the lowest next task timestamp of all domains.
            }
        }

        long wait = next - System.currentTimeMillis();
        if(wait == 0) return;

        synchronized(thread) {
            thread.wait(wait);
        }
    }

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
