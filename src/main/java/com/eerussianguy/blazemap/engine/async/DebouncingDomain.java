package com.eerussianguy.blazemap.engine.async;

import java.util.*;
import java.util.function.Consumer;

public class DebouncingDomain<T> {
    private static final ThreadLocal<List<Object>> PENDING = ThreadLocal.withInitial(ArrayList::new);

    /** The DebouncingThread that makes our Domain work. */
    private final DebouncingThread debouncer;

    /** Map of pending tasks. Keys are the actual tasks themselves, values are each task's delay configuration. */
    private final Map<T, DebouncingDelay> pendingTasks = new HashMap<>();

    /** The callback that takes tasks that are due for execution. */
    private final Consumer<T> callback;

    /** Delay config: How much to delay execution each time a task is touched. Actual delay is 1-2x this */
    private final int delayStep;

    /** Delay config: How much a task can be delayed until further delays are ignored. */
    private final int maxDelay;

    /** Timestamp of the next expected execution of a task within this domain. */
    private long nextTaskTimestamp = Long.MAX_VALUE;

    public DebouncingDomain(DebouncingThread debouncer, Consumer<T> callback, int delayStep, int maxDelay) {
        this.debouncer = debouncer;
        this.callback = callback;
        this.delayStep = delayStep;
        this.maxDelay = maxDelay;
        debouncer.add(this);
    }

    /**
     * Enqueue a new task, if it is not already enqueued, with a fresh delay configuration.
     * Otherwise, if it already exists, attempt to delay the tasks further.
     */
    public void push(T task) {
        synchronized(pendingTasks) { // Get or create task delay, and touch it.
            DebouncingDelay delay = pendingTasks.computeIfAbsent(task, $ -> DebouncingDelay.get(delayStep, maxDelay)).touch();
            long executionTimestamp = delay.getExecutionTimestamp();
            if(executionTimestamp < this.nextTaskTimestamp) {
                this.nextTaskTimestamp = executionTimestamp;
                debouncer.nextTask(executionTimestamp);
            }
        }
    }

    /** Remove an existing task. */
    public boolean remove(T task) {
        synchronized(pendingTasks) {
            return pendingTasks.remove(task) != null;
        }
    }

    /** Clear the entire task queue by voiding its contents. Ideal for volatile work that we can afford to lose. */
    public void clear() {
        synchronized(pendingTasks) {
            pendingTasks.clear();
        }
    }

    /** Clear the entire task queue by executing everything. Necessary for valuable work we cannot afford to lose. */
    public void finish() {
        // Use Long.MAX_VALUE to force execution of every task regardless of how much it has been delayed.
        executePendingTasks(Long.MAX_VALUE);
    }

    /** Get the size of the task queue. */
    public int size() {
        return pendingTasks.size();
    }

    /**
     * Execute and remove all due tasks and return the timestamp of the next expected execution.
     * @param now timestamp we consider as the current point in time. Any tasks due before or at this point will execute.
     * @return timestamp of the next due execution of a task within this domain.
     */
    public long executePendingTasks(long now) {
        // No pending tasks to execute, just skip work.
        if(this.nextTaskTimestamp > now){
            return this.nextTaskTimestamp;
        }

        // Pending tasks to execute -- Uses a ThreadLocal ArrayList to minimize allocations and maintain thread safety.
        List<T> pending = (List<T>) PENDING.get();

        // Loop over all tasks. Pull pending ones aside, and determine time of next execution
        long nextTaskTimestamp = Long.MAX_VALUE;
        synchronized(pendingTasks) {
            // Use iterator instead of foreach so that we can remove entries while iterating over them
            Iterator<Map.Entry<T, DebouncingDelay>> tasks = pendingTasks.entrySet().iterator();

            while(tasks.hasNext()) {
                Map.Entry<T, DebouncingDelay> entry = tasks.next();
                DebouncingDelay delay = entry.getValue();
                long executionTimestamp = delay.getExecutionTimestamp();
                if(executionTimestamp <= now) {
                    pending.add(entry.getKey()); // Put the task in the list to be executed immediately
                    delay.release(); // Put it back in the object pool to reuse later instead of allocating a new one.
                    tasks.remove(); // Remove this entry from the pending tasks queue
                }
                else if(executionTimestamp < nextTaskTimestamp) { // Find timestamp of the next due task.
                    nextTaskTimestamp = executionTimestamp;
                }
            }
            this.nextTaskTimestamp = nextTaskTimestamp;
        }

        // Execute all pending tasks
        for(T task : pending) {
            try {
                callback.accept(task);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        // Clear the ThreadLocal ArrayList for the next iteration in this thread.
        pending.clear();

        // Return timestamp of next task
        return this.nextTaskTimestamp;
    }
}
