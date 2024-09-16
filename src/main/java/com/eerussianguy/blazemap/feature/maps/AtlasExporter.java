package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.StorageAccess;
import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import com.eerussianguy.blazemap.engine.client.LayerRegionTile;
import com.eerussianguy.blazemap.util.Colors;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AtlasExporter {
    private static Task task = null;

    public static synchronized void exportAsync(Task task) {
        if(AtlasExporter.task == null) {
            AtlasExporter.task = task;
            BlazeMapAsync.instance().clientChain.runOnDataThread(AtlasExporter::exportAsync);
        } else {
            AtlasExporter.task.flashUntil = 3 + (int)(System.currentTimeMillis() / 1000L);
        }
    }

    public static synchronized Task getTask() {
        return task;
    }

    private static synchronized void resetTask() {
        task = null;
    }

    private static void exportAsync() {
        task.setStage(Task.Stage.CALCULATING);
        TileResolution resolution = TileResolution.FULL;
        StorageAccess.Internal storage = BlazeMapClientEngine.getDimensionStorage(task.dimension);
        NativeImage atlas = constructAtlas(storage, resolution);

        try(atlas) {
            task.setStage(Task.Stage.STITCHING);
            var layerKeys = task.map.value().getLayers();
            for(var layerKey : layerKeys) {
                if(!task.layers.contains(layerKey)) continue;
                File folder = storage.getMipmap(layerKey.location, ".", resolution);
                for(int regionX = task.atlasStartX; regionX <= task.atlasEndX; regionX++) {
                    for(int regionZ = task.atlasStartZ; regionZ <= task.atlasEndZ; regionZ++) {
                        File file = new File(folder, LayerRegionTile.getImageName(new RegionPos(regionX, regionZ)));
                        if(!file.exists()) continue;
                        NativeImage tile = NativeImage.read(Files.newInputStream(file.toPath()));

                        int regionOffsetX = (regionX - task.atlasStartX) * resolution.regionWidth;
                        int regionOffsetZ = (regionZ - task.atlasStartZ) * resolution.regionWidth;
                        for(int x = 0; x < resolution.regionWidth; x++) {
                            for(int z = 0; z < resolution.regionWidth; z++) {
                                int atlasPixelX = regionOffsetX + x;
                                int atlasPixelZ = regionOffsetZ + z;
                                int atlasPixel = atlas.getPixelRGBA(atlasPixelX, atlasPixelZ);
                                int tilePixel = tile.getPixelRGBA(x, z);
                                atlas.setPixelRGBA(atlasPixelX, atlasPixelZ, Colors.layerBlend(atlasPixel, tilePixel));
                            }
                        }

                        // non-atomic op on volatile int is ok because only 1 thread writes to variable
                        // Java guarantees r/w access to 32-bit variables is atomic, so other threads will read either old or new value with no need for synchronization and no risk of corruption.
                        task.tilesCurrent++;
                    }
                }
            }

            task.setStage(Task.Stage.SAVING);
            File file = getExportFile();
            file.getParentFile().mkdirs();
            atlas.writeToFile(file);
        } catch (IOException e) {
            BlazeMap.LOGGER.error("Error in AtlasExporter", e);
            task.errored = true;
            try { Thread.sleep(500); }
            catch(InterruptedException ignored){}
        } finally {
            resetTask();
        }
    }

    private static File getExportFile() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return new File(Minecraft.getInstance().gameDirectory, String.format("screenshots/%04d-%02d-%02d_%02d.%02d.%02d-blazemap-export.png", year, month, day, hour, minute, second));
    }

    private static NativeImage constructAtlas(StorageAccess.Internal storage, TileResolution resolution) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int regionsX, regionsZ;

        var layers = task.map.value().getLayers();
        for(var layer : layers) {
            if(!task.layers.contains(layer)) continue;
            File folder = storage.getMipmap(layer.location, ".", resolution);
            File[] images = folder.listFiles();
            if(images == null) continue;

            for(var image : images) {
                String filename = image.getName();
                if(!filename.endsWith(".png")) continue; // skip buffers
                String[] coords = filename.replaceAll("(^\\[)|(]\\.png$)", "").split(",");
                if(coords.length != 2) continue;
                // non-atomic op on volatile int is ok because only 1 thread writes to variable
                // Java guarantees r/w access to 32-bit variables is atomic, so other threads will read either old or new value with no need for synchronization and no risk of corruption.
                task.tilesTotal++;
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                if(x < minX) minX = x;
                if(x > maxX) maxX = x;
                if(z < minZ) minZ = z;
                if(z > maxZ) maxZ = z;
            }
        }

        regionsX = 1 + maxX - minX;
        regionsZ = 1 + maxZ - minZ;
        task.atlasStartX = minX;
        task.atlasStartZ = minZ;
        task.atlasEndX = maxX;
        task.atlasEndZ = maxZ;

        return new NativeImage(NativeImage.Format.RGBA, resolution.regionWidth*regionsX, resolution.regionWidth*regionsZ, true);
    }

    public static class Task {
        public final ResourceKey<Level> dimension;
        public final BlazeRegistry.Key<MapType> map;
        public final List<BlazeRegistry.Key<Layer>> layers;
        private volatile int tilesTotal, tilesCurrent;
        private volatile Stage stage = Stage.QUEUED;
        private volatile int flashUntil = 0;
        private int atlasStartX, atlasStartZ;
        private int atlasEndX, atlasEndZ;
        private volatile boolean errored;

        public Task(ResourceKey<Level> dimension, BlazeRegistry.Key<MapType> map, List<BlazeRegistry.Key<Layer>> layers) {
            this.dimension = dimension;
            this.map = map;
            this.layers = new ArrayList<>(layers);
        }

        public int getTilesTotal() {
            return tilesTotal;
        }

        public int getTilesCurrent() {
            return tilesCurrent;
        }

        private synchronized void setStage(Stage stage) {
            this.stage = stage;
        }

        public synchronized Stage getStage() {
            return stage;
        }

        public int getFlashUntil() {
            return flashUntil;
        }

        public boolean isErrored() {
            return errored;
        }

        public enum Stage {
            QUEUED,
            CALCULATING,
            STITCHING,
            SAVING
        }
    }
}
