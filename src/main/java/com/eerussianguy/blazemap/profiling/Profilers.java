package com.eerussianguy.blazemap.profiling;

import com.eerussianguy.blazemap.profiling.Profiler.*;

public class Profilers {
    public static final TimeProfilerSync DEBUG_TIME_PROFILER = new TimeProfilerSync("debug", 60);

    public static class Server {
        public static final TimeProfilerSync COLLECTOR_TIME_PROFILER = new TimeProfilerSync("server_collector", 20);
        public static final LoadProfiler COLLECTOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync PROCESSOR_TIME_PROFILER = new TimeProfilerAsync("server_processor", 20);
        public static final LoadProfiler PROCESSOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TRANSFORMER_TIME_PROFILER = new TimeProfilerAsync("server_transformer", 20);
        public static final LoadProfiler TRANSFORMER_LOAD_PROFILER = new LoadProfiler(20, 50);

        public static class Mixin {
            public static final TimeProfilerSync CHUNKHOLDER_TIME_PROFILER = new TimeProfilerSync("chunkholder", 20);
            public static final LoadProfiler CHUNKHOLDER_LOAD_PROFILER = new LoadProfiler(20, 50);
            public static final TimeProfilerSync CHUNKPACKET_TIME_PROFILER = new TimeProfilerSync("chunkpacket", 20);
            public static final LoadProfiler CHUNKPACKET_LOAD_PROFILER = new LoadProfiler(20, 50);
        }
    }

    public static class Client {
        public static final TimeProfilerSync COLLECTOR_TIME_PROFILER = new TimeProfilerSync("client_collector", 20);
        public static final LoadProfiler COLLECTOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync PROCESSOR_TIME_PROFILER = new TimeProfilerAsync("client_processor", 20);
        public static final LoadProfiler PROCESSOR_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TRANSFORMER_TIME_PROFILER = new TimeProfilerAsync("client_transformer", 20);
        public static final LoadProfiler TRANSFORMER_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync LAYER_TIME_PROFILER = new TimeProfilerAsync("layer", 20);
        public static final LoadProfiler LAYER_LOAD_PROFILER = new LoadProfiler(20, 50);
        public static final TimeProfilerAsync TILE_TIME_PROFILER = new TimeProfilerAsync("tile", 60);
        public static final LoadProfiler TILE_LOAD_PROFILER = new LoadProfiler(60, 1000);

        public static class Mixin {
            public static final TimeProfilerSync RENDERCHUNK_TIME_PROFILER = new TimeProfilerSync("renderchunk", 20);
            public static final LoadProfiler RENDERCHUNK_LOAD_PROFILER = new LoadProfiler(20, 50);
            public static final TimeProfilerSync SODIUM_TIME_PROFILER = new TimeProfilerSync("sodium_mixin", 20);
            public static final LoadProfiler SODIUM_LOAD_PROFILER = new LoadProfiler(20, 50);
        }
    }

    public static class Minimap {
        public static final TimeProfilerSync DRAW_TIME_PROFILER = new TimeProfilerSync("draw", 60);
        public static final TimeProfilerSync TEXTURE_TIME_PROFILER = new TimeProfilerSync("texture", 60);
        public static final LoadProfiler TEXTURE_LOAD_PROFILER = new LoadProfiler(60, 16);
    }

    public static class FileOps {
        public static final TimeProfilerAsync CACHE_READ_TIME_PROFILER = new TimeProfilerAsync("cache_read", 20);
        public static final TimeProfilerAsync CACHE_WRITE_TIME_PROFILER = new TimeProfilerAsync("cache_write", 20);
        public static final TimeProfilerAsync LAYER_READ_TIME_PROFILER = new TimeProfilerAsync("layer_read", 20);
        public static final TimeProfilerAsync LAYER_WRITE_TIME_PROFILER = new TimeProfilerAsync("layer_write", 20);
    }
}
