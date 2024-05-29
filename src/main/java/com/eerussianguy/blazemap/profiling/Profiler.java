package com.eerussianguy.blazemap.profiling;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

public abstract class Profiler {
    protected long[] roll;
    protected long min, max;
    protected double avg;
    protected int idx;

    public synchronized double getAvg() {
        return avg;
    }

    public synchronized double getMin() {
        return min;
    }

    public synchronized double getMax() {
        return max;
    }

    protected void recalculate() {
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
        }
    }

    /**
     * This is the Minecraft profiler triggered by F3 + L
     *
     * @return ProfilerFiller for the current side
     */
    protected ProfilerFiller getMCProfiler() {
        // Minecraft runs a separate profiler for the Client and Server thread
        // TODO: Figure out how to access the actual server profiler
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            return InactiveProfiler.INSTANCE;
        }
        return Minecraft.getInstance().getProfiler();
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
                recalculate();
            }
            else {
                long delta = System.nanoTime() - start;
                Arrays.fill(roll, delta);
                synchronized(this) {
                    avg = min = max = delta;
                }
                populated = true;
            }
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
                recalculate();
            }
            else {
                long delta = System.nanoTime() - start.get();
                Arrays.fill(roll, delta);
                avg = min = max = delta;
                populated = true;
            }
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
            recalculate();
        }
    }
}
