package com.eerussianguy.blazemap.api.util;

import java.io.*;
import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.api.BlazeRegistry;

public class MinecraftStreams {
    public static class Output extends DataOutputStream {
        public Output(OutputStream out) {
            super(out);
        }

        public <T> void writeCollection(Collection<T> collection, IOConsumer<T> consumer) throws IOException {
            writeInt(collection.size());
            for(T element : collection) {
                consumer.accept(element);
            }
        }

        public void writeResourceLocation(ResourceLocation resourceLocation) throws IOException {
            writeUTF(resourceLocation.toString());
        }

        public <T> void writeKey(BlazeRegistry.Key<T> key) throws IOException {
            writeResourceLocation(key.location);
        }

        public void writeBlockPos(BlockPos pos) throws IOException {
            writeLong(pos.asLong());
        }

        public void writeChunkPos(ChunkPos pos) throws IOException {
            writeLong(pos.toLong());
        }

        public void writeRegionPos(RegionPos pos) throws IOException {
            writeInt(pos.x);
            writeInt(pos.z);
        }
    }

    public static class Input extends DataInputStream {
        public Input(InputStream in) {
            super(in);
        }

        public void readCollection(IORunnable function) throws IOException {
            int count = readInt();
            for(int i = 0; i < count; i++) {
                function.run();
            }
        }

        public ResourceLocation readResourceLocation() throws IOException {
            return new ResourceLocation(readUTF());
        }

        public <T> BlazeRegistry.Key<T> readKey(BlazeRegistry<T> registry) throws IOException {
            ResourceLocation location = readResourceLocation();
            return registry.findOrCreate(location);
        }

        public BlockPos readBlockPos() throws IOException {
            return BlockPos.of(readLong());
        }

        public ChunkPos readChunkPos() throws IOException {
            return new ChunkPos(readLong());
        }

        public RegionPos readRegionPos() throws IOException {
            int x = readInt();
            int z = readInt();
            return new RegionPos(x, z);
        }
    }

    @FunctionalInterface
    public interface IORunnable {
        void run() throws IOException;
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }
}