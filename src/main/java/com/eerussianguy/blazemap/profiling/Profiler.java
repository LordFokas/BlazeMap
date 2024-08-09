package com.eerussianguy.blazemap.profiling;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class Profiler {
    protected AtomicLongArray roll;
    protected AtomicInteger idx = new AtomicInteger(0);

    protected volatile long min, max;
    protected volatile double avg;
    protected volatile boolean isDirty = false;

    protected static MinecraftServer serverInstance = null;

    /**
     * Must call updateSummaryStats() before this to synchronise the value
     */
    public double getAvg() {
        return avg;
    }

    /**
     * Must call updateSummaryStats() before this to synchronise the value
     */
    public double getMin() {
        return min;
    }

    /**
     * Must call updateSummaryStats() before this to synchronise the value
     */
    public double getMax() {
        return max;
    }

    /**
     * This should only be called just before accessing the max, min, and avg
     * to avoid unnecessary thread synchronisations when the debugger is shut.
     */
    public void updateSummaryStats() {
        if (!isDirty) return;

        double sum = 0;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;

        for(int v = 0; v < roll.length(); v++) {
            long value = roll.get(v);

            if(value < min) min = value;
            if(value > max) max = value;
            sum += value;
        }

        synchronized(this) {
            this.avg = sum / roll.length();
            this.min = min;
            this.max = max;
            this.isDirty = false;
        }
    }

    /**
     * This is the Minecraft profiler triggered by F3 + L.
     * This method is public static so temporary profiles can easily be sprinkled about
     * while debugging.
     *
     * @return ProfilerFiller for the current thread, if on a thread with a profiler
     */
    public static ProfilerFiller getMCProfiler() {
        // Short circuit to avoid spending time on string comparisons when profiler not active
        if (Minecraft.getInstance().getProfiler() == InactiveProfiler.INSTANCE) {
            return InactiveProfiler.INSTANCE;

        // Minecraft runs a separate profiler for the Client and Server thread.
        // Also, the Minecraft profiler only works in the context of those two main game threads.
        } else if (Thread.currentThread().getName().equals("Render thread")) {
            return Minecraft.getInstance().getProfiler();

        } else if (Thread.currentThread().getName().equals("Server thread") && serverInstance != null) {
            return serverInstance.getProfiler();
        }

        return InactiveProfiler.INSTANCE;
    }

    /**
     * We need to set a reference to the MinecraftServer itself instead of just
     * its profiler as its profiler reference will be swapped out by Minecraft
     * when profiling starts and stops
     */
    public static void setServerInstance(MinecraftServer server) {
        serverInstance = server;
    }


    public static abstract class TimeProfiler extends Profiler {
        protected boolean populated = false;
        protected String profilerName;

        public TimeProfiler(String profilerName, int rollSize) {
            this.roll = new AtomicLongArray(rollSize);
            this.profilerName = "BlazeMap_" + profilerName;
        }

        public abstract void begin();

        public abstract void end();

        public static class Dummy extends TimeProfiler {
            public Dummy() {
                super("DUMMY", 20);
            }

            @Override
            public void begin() {}

            @Override
            public void end() {}
        }
    }

    public static class TimeProfilerSync extends TimeProfiler {
        private long start;

        public TimeProfilerSync(String profilerName, int rollSize) {
            super(profilerName, rollSize);
        }

        @Override
        public void begin() {
            start = System.nanoTime();
            getMCProfiler().push(profilerName);
        }

        @Override
        public void end() {
            getMCProfiler().pop();

            if(populated) {
                roll.set(idx.get(), System.nanoTime() - start);
                idx.getAndUpdate((int i) -> (i + 1) % roll.length());
            }
            else {
                long delta = System.nanoTime() - start;
                for (int i = 0; i < roll.length(); i++) {
                    roll.set(i, delta);
                }
                synchronized(this) {
                    avg = min = max = delta;
                }
                populated = true;
            }
            isDirty = true;
        }
    }

    public static class TimeProfilerAsync extends TimeProfiler {
        private final ThreadLocal<Long> start = new ThreadLocal<>();

        public TimeProfilerAsync(String profilerName, int rollSize) {
            super(profilerName, rollSize);
        }

        @Override
        public void begin() {
            start.set(System.nanoTime());
        }

        @Override
        public void end() {
            if(populated) {
                roll.set(idx.get(), System.nanoTime() - start.get());
                idx.getAndUpdate((int i) -> (i + 1) % roll.length());
            }
            else {
                long delta = System.nanoTime() - start.get();
                for (int i = 0; i < roll.length(); i++) {
                    roll.set(i, delta);
                }

                avg = min = max = delta;
                populated = true;
            }
            isDirty = true;
        }
    }

    public static class LoadProfiler extends Profiler {
        public final int interval;
        public final String unit;
        public final String span;
        private long last;

        public LoadProfiler(int rollSize, int interval) {
            this.roll = new AtomicLongArray(rollSize);
            this.interval = interval;
            switch(interval) {
                case 16 -> this.unit = "f";
                case 50 -> this.unit = "t";
                case 1000 -> this.unit = "s";
                default -> this.unit = "?";
            }

            double window = interval * rollSize;
            if(window < 950){
                span = String.format("%d ms", Math.round(window));
                return;
            }
            window /= 1000;
            if(window < 59.5){
                span = String.format("%d sec", Math.round(window));
                return;
            }
            window /= 60;
            span = String.format("%d min", Math.round(window));
        }

        public void hit() {
            update(1);
        }

        public void ping() {
            update(0);
            updateSummaryStats();
        }

        private void update(int i) {
            int index = idx.get();
            long now = System.currentTimeMillis() / interval;

            // I believe this should be thread safe, as only one thread should be able to
            // advance the idx if we're in the next time block
            if(now != last && idx.compareAndSet(index, (index + 1) % roll.length())) {
                roll.set(idx.get(), i);
                last = now;
                isDirty = true;
            }
            else {
                if(i == 0) return;
                roll.getAndAdd(index, i);
                isDirty = true;
            }
        }
    }
}
