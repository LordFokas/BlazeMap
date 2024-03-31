package com.eerussianguy.blazemap.util;

import com.eerussianguy.blazemap.util.Profiler.*;

public class Profilers {
    public static final TimeProfilerSync DEBUG_TIME_PROFILER = new TimeProfilerSync(60);

    public static class Server {
        public static final TimeProfilerSync COLLECTOR_TIME_PROFILER = new TimeProfilerSync(20);
        public static final LoadProfiler COLLECTOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync PROCESSOR_TIME_PROFILER = new TimeProfilerAsync(20);
        public static final LoadProfiler PROCESSOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TRANSFORMER_TIME_PROFILER = new TimeProfilerAsync(20);
        public static final LoadProfiler TRANSFORMER_LOAD_PROFILER = new LoadProfiler(20, 50);

        public static final TimeProfilerSync TRIGGER_CHUNK_DIRTY_TIME_PROFILER = new TimeProfilerSync(20);
        public static final LoadProfiler TRIGGER_CHUNK_DIRTY_LOAD_PROFILER = new LoadProfiler(20, 50);
    }

    public static class Client {
        public static final TimeProfilerSync COLLECTOR_TIME_PROFILER = new TimeProfilerSync(20);
        public static final LoadProfiler COLLECTOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync PROCESSOR_TIME_PROFILER = new TimeProfilerAsync(20);
        public static final LoadProfiler PROCESSOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TRANSFORMER_TIME_PROFILER = new TimeProfilerAsync(20);
        public static final LoadProfiler TRANSFORMER_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync LAYER_TIME_PROFILER = new TimeProfilerAsync(20);
        public static final LoadProfiler LAYER_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TILE_TIME_PROFILER = new TimeProfilerAsync(60);
        public static final LoadProfiler TILE_LOAD_PROFILER = new LoadProfiler(60, 1000);
    }

    public static class Minimap {
        public static final TimeProfilerSync DRAW_TIME_PROFILER = new TimeProfilerSync(60);
        public static final TimeProfilerSync TEXTURE_TIME_PROFILER = new TimeProfilerSync(60);
        public static final LoadProfiler TEXTURE_LOAD_PROFILER = new LoadProfiler(60, 16);
    }
}
