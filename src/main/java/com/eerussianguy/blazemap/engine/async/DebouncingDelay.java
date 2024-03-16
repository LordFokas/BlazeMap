package com.eerussianguy.blazemap.engine.async;

import java.util.ArrayList;

public class DebouncingDelay {
    private static final int MAX_POOL = 1000;
    private static final ArrayList<DebouncingDelay> POOL = new ArrayList<>();

    /** How much to further delay execution when touch() is called. Actual delay is 1-2x this. */
    private int step;

    /** Latest point at which execution will be allowed. Further delay will be ignored, as all tasks must execute eventually */
    private long latestExecutionTimestamp;

    /** Timestamp at which this task is expected to execute. Will change if touch() is called and the limit has not been reached. */
    private long executionTimestamp;

    /**
     * Get a new DebouncingDelay.
     * If one is available in the object pool, return it; otherwise, allocate a new one.
     */
    public static DebouncingDelay get(int step, int limit) {
        synchronized(POOL){
            int size = POOL.size();
            if(size > 0){
                // Remove last to avoid having to touch other entry indices.
                return POOL.remove(size-1).configure(step, limit);
            }
        }
        return new DebouncingDelay().configure(step, limit);
    }

    /**
     * Configure the object.
     * This, instead of setting with a constructor, allows reusing objects.
     */
    private DebouncingDelay configure(int step, int limit) {
        long now = System.currentTimeMillis();
        this.step = step;
        this.executionTimestamp = now + step;
        this.latestExecutionTimestamp = now + limit;
        return this;
    }

    /** Push the execution timestamp forward, if possible. */
    public DebouncingDelay touch() {
        long now = System.currentTimeMillis();

        // Add another random fraction of a step to spread out sibling tasks in the timeline and further smooth CPU load.
        long random = System.nanoTime() % step;
        executionTimestamp = Math.min(now + step + random, latestExecutionTimestamp);

        return this;
    }

    /**
     * Release this object back to the available object pool.
     * Please make sure to drop any references to this object after calling this,
     * so as to not call any methods / make any changes in an "available" object.
     */
    public void release(){
        synchronized(POOL){
            if(POOL.size() < MAX_POOL){
                POOL.add(this);
            }
        }
    }

    /** Get the timestamp at which this task is expected to execute. */
    public long getExecutionTimestamp(){
        return executionTimestamp;
    }
}
