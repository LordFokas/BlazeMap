package com.eerussianguy.blazemap.engine.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.minecraft.world.level.ChunkPos;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.PixelSource;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.StorageAccess;
import com.eerussianguy.blazemap.profiling.Profilers;
import com.mojang.blaze3d.platform.NativeImage;

public class LayerRegionTile {
    private static final Object MUTEX = new Object();
    private static volatile int instances = 0, loaded = 0;

    private final ReentrantReadWriteLock imageLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();
    private final ReentrantLock bufferLock = new ReentrantLock();

    private final File file, buffer;
    private NativeImage image;
    private final PixelSource imageWrapper;
    private final TileResolution resolution;
    private volatile boolean isEmpty = true;
    private volatile boolean isDirty = false;
    private volatile boolean destroyed = false;

    public static String getImageName(RegionPos region) {
        return region + ".png";
    }

    private static String getBufferName(RegionPos region) {
        return region + ".buffer";
    }

    public LayerRegionTile(StorageAccess.Internal storage, BlazeRegistry.Key<Layer> layer, RegionPos region, TileResolution resolution) {
        this.file = storage.getMipmap(layer.location, getImageName(region), resolution);
        this.buffer = storage.getMipmap(layer.location, getBufferName(region), resolution);
        this.resolution = resolution;

        imageWrapper = new PixelSource() {
            @Override
            public int getPixel(int x, int y) {
                return image.getPixelRGBA(x, y);
            }

            @Override
            public int getWidth() {
                return image.getWidth();
            }

            @Override
            public int getHeight() {
                return image.getHeight();
            }
        };
    }

    public void tryLoad() {
        if(file.exists()) {
            Profilers.FileOps.LAYER_READ_TIME_PROFILER.begin();

            // Trying to greedily acquire the locks with `tryLock()` to minimise delay on game thread
            // TODO: Figure out a "failure" case so the timed tryLock can be used instead to "give up"
            // if it's waited too long, thereby not blocking the tick
            if (!fileLock.readLock().tryLock()) fileLock.readLock().lock();
            if (!imageLock.writeLock().tryLock()) imageLock.writeLock().lock();

            try {
                image = NativeImage.read(Files.newInputStream(file.toPath()));
                isEmpty = false;
            }
            catch(IOException e) {
                // FIXME: this needs to hook into a reporting mechanism AND possibly automated LRT regeneration
                BlazeMap.LOGGER.error("Error loading LayerRegionTile: {}", file, e);
            }
            finally {
                imageLock.writeLock().unlock();
                fileLock.readLock().unlock();
                Profilers.FileOps.LAYER_READ_TIME_PROFILER.end();
            }
        }
        else {
            file.getParentFile().mkdirs();
        }
        onCreate();
    }

    public void save() {
        if(isEmpty || !isDirty) return;

        // Save image into buffer
        Profilers.FileOps.LAYER_WRITE_TIME_PROFILER.begin();

        bufferLock.lock();
        try {
            imageLock.readLock().lock();
            try {
                image.writeToFile(buffer);
                isDirty = false;
            }
            catch(IOException e) {
                // FIXME: this needs to hook into a reporting mechanism
                BlazeMap.LOGGER.error("Error saving LayerRegionTile buffer: {}", buffer, e);
                e.printStackTrace();
                return;
            }
            finally {
                imageLock.readLock().unlock();
            }

            // Move buffer to real image path
            fileLock.writeLock().lock();
            try {
                Files.move(buffer.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch(IOException e) {
                // FIXME: this needs to hook into a reporting mechanism
                BlazeMap.LOGGER.error("Error moving LayerRegionTile buffer to image: {} {}", buffer, file, e);
                e.printStackTrace();
            }
            finally {
                fileLock.writeLock().unlock();
            }
        } finally {
            bufferLock.unlock();
            Profilers.FileOps.LAYER_WRITE_TIME_PROFILER.end();
        }
    }

    public void updateTile(NativeImage tile, ChunkPos chunk) {
        int xOffset = (chunk.getRegionLocalX() << 4) / resolution.pixelWidth;
        int zOffset = (chunk.getRegionLocalZ() << 4) / resolution.pixelWidth;
        boolean wasEmpty = isEmpty;

        // Since isDirty is a volatile bool for thread safety reasons, it's slightly more efficient to only update it once
        // as updating it flushes the CPU write cache. So using a local var to track dirtiness until final value ready
        boolean dirty = isDirty;

        imageLock.writeLock().lock();
        try {
            if(isEmpty) {
                image = new NativeImage(NativeImage.Format.RGBA, resolution.regionWidth, resolution.regionWidth, true);
                isEmpty = false;
            }

            for(int x = 0; x < resolution.chunkWidth; x++) {
                for(int z = 0; z < resolution.chunkWidth; z++) {
                    int old = image.getPixelRGBA(xOffset + x, zOffset + z);
                    int pixel = tile.getPixelRGBA(x, z);
                    if(pixel != old) {
                        image.setPixelRGBA(xOffset + x, zOffset + z, pixel);
                        dirty = true;
                    }
                }
            }
        }
        finally {
            isDirty = dirty;
            imageLock.writeLock().unlock();
        }

        if(wasEmpty && !isEmpty) {
            onFill();
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void consume(Consumer<PixelSource> consumer) {
        if(isEmpty) return;

        imageLock.readLock().lock();
        try {
            consumer.accept(imageWrapper);
        }
        finally {
            imageLock.readLock().unlock();
        }
    }

    private void onCreate() {
        synchronized(MUTEX) {
            instances++;
            if(!isEmpty) {
                loaded += resolution.regionSizeKb;
            }
        }
    }

    private void onFill() {
        synchronized(MUTEX) {
            loaded += resolution.regionSizeKb;
        }
    }

    public static int getInstances() {
        synchronized(MUTEX) {
            return instances;
        }
    }

    public static int getLoadedKb() {
        synchronized(MUTEX) {
            return loaded;
        }
    }

    public void destroy() {
        if(destroyed) return;

        // Update static vars
        synchronized(MUTEX) {
            instances--;
            if(!isEmpty) {
                loaded -= resolution.regionSizeKb;
            }
        }

        imageLock.writeLock().lock();
        try {
            save();

            if(image != null) {
                image.close();
                image = null;
            }

            isDirty = false;
            isEmpty = true;
            destroyed = true;
        }
        finally {
            imageLock.writeLock().unlock();
        }
    }
}
