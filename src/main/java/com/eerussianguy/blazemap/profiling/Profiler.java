package com.eerussianguy.blazemap.profiling;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class Profiler {
    protected long[] roll;
    protected int idx;

    protected long min, max;
    protected double avg;
    protected boolean isDirty = false;

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
     * to avoid unnecessary work when the debugger is shut.
     */
    public void updateSummaryStats() {
        if (!isDirty) return;

        double sum = 0;
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for(long v : roll) {
            if(v < min) min = v;
            if(v > max) max = v;
            sum += v;
        }

        synchronized(this) {
            this.avg = sum / roll.length;
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
        // Minecraft runs a separate profiler for the Client and Server thread.
        // Also, the Minecraft profiler only works in the context of those two main game threads.
        if (Thread.currentThread().getName().equals("Render thread")) {
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
            this.roll = new long[rollSize];
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
                roll[idx] = System.nanoTime() - start;
                idx = (idx + 1) % roll.length;
            }
            else {
                long delta = System.nanoTime() - start;
                Arrays.fill(roll, delta);
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
        public synchronized void end() {
            if(populated) {
                roll[idx] = System.nanoTime() - start.get();
                idx = (idx + 1) % roll.length;
            }
            else {
                long delta = System.nanoTime() - start.get();
                Arrays.fill(roll, delta);
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
            this.roll = new long[rollSize];
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

        private synchronized void update(int i) {
            long now = System.currentTimeMillis() / interval;
            if(now == last) {
                if(i == 0) return;
                roll[idx] += i;
            }
            else {
                idx = (idx + 1) % roll.length;
                roll[idx] = i;
                last = now;
            }
            isDirty = true;
        }
    }
}
