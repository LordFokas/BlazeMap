package com.eerussianguy.blazemap.feature.maps;

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
import java.util.Date;
import java.util.List;

public class AtlasExporter {
    private static Task task = null;

    public static boolean export(Task task) {
        if(AtlasExporter.task == null) {
            AtlasExporter.task = task;
            BlazeMapAsync.instance().clientChain.runOnDataThread(AtlasExporter::export);
            return true;
        }
        return false;
    }

    private static void export() {
        TileResolution resolution = TileResolution.FULL;
        StorageAccess.Internal storage = BlazeMapClientEngine.getDimensionStorage(task.dimension);
        AtlasInfo atlasInfo = new AtlasInfo();
        NativeImage atlas = constructAtlas(storage, resolution, atlasInfo);

        try(atlas) {
            var layerKeys = task.map.value().getLayers();
            for(var layerKey : layerKeys) {
                if(!task.layers.contains(layerKey)) continue;
                File folder = storage.getMipmap(layerKey.location, ".", resolution);
                for(int regionX = atlasInfo.atlasStartX; regionX <= atlasInfo.atlasEndX; regionX++) {
                    for(int regionZ = atlasInfo.atlasStartZ; regionZ <= atlasInfo.atlasEndZ; regionZ++) {
                        File file = new File(folder, LayerRegionTile.getImageName(new RegionPos(regionX, regionZ)));
                        if(!file.exists()) continue;
                        NativeImage tile = NativeImage.read(Files.newInputStream(file.toPath()));

                        int regionOffsetX = (regionX - atlasInfo.atlasStartX) * resolution.regionWidth;
                        int regionOffsetZ = (regionZ - atlasInfo.atlasStartZ) * resolution.regionWidth;
                        for(int x = 0; x < resolution.regionWidth; x++) {
                            for(int z = 0; z < resolution.regionWidth; z++) {
                                int atlasPixelX = regionOffsetX + x;
                                int atlasPixelZ = regionOffsetZ + z;
                                int atlasPixel = atlas.getPixelRGBA(atlasPixelX, atlasPixelZ);
                                int tilePixel = tile.getPixelRGBA(x, z);
                                atlas.setPixelRGBA(atlasPixelX, atlasPixelZ, Colors.layerBlend(atlasPixel, tilePixel));
                            }
                        }
                    }
                }
            }

            File file = new File(Minecraft.getInstance().gameDirectory, "screenshots/blazemap-export-"+ (System.currentTimeMillis() / 1000L) +".png");
            file.getParentFile().mkdirs();
            atlas.writeToFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        task = null;
    }

    private static NativeImage constructAtlas(StorageAccess.Internal storage, TileResolution resolution, AtlasInfo atlasInfo) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int regionsX, regionsZ;

        for(var layer : task.layers) {
            File folder = storage.getMipmap(layer.location, ".", resolution);
            File[] images = folder.listFiles();
            if(images == null) continue;

            for(var image : images) {
                String[] coords = image.getName().replaceAll("(^\\[)|(]\\.png$)", "").split(",");
                if(coords.length != 2) continue;
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
        atlasInfo.atlasStartX = minX;
        atlasInfo.atlasStartZ = minZ;
        atlasInfo.atlasEndX = maxX;
        atlasInfo.atlasEndZ = maxZ;

        return new NativeImage(NativeImage.Format.RGBA, resolution.regionWidth*regionsX, resolution.regionWidth*regionsZ, true);
    }

    private static class AtlasInfo {
        public int atlasStartX, atlasStartZ;
        public int atlasEndX, atlasEndZ;
    }

    public static class Task {
        public final ResourceKey<Level> dimension;
        public final BlazeRegistry.Key<MapType> map;
        public final List<BlazeRegistry.Key<Layer>> layers;

        public Task(ResourceKey<Level> dimension, BlazeRegistry.Key<MapType> map, List<BlazeRegistry.Key<Layer>> layers) {
            this.dimension = dimension;
            this.map = map;
            this.layers = new ArrayList<>(layers);
        }
    }
}
