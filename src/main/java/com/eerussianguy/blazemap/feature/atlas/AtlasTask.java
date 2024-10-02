package com.eerussianguy.blazemap.feature.atlas;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.api.maps.MapType;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.StorageAccess;
import com.eerussianguy.blazemap.engine.client.BlazeMapClientEngine;
import com.eerussianguy.blazemap.engine.client.LayerRegionTile;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.mojang.blaze3d.platform.NativeImage;

public class AtlasTask {
    public final ResourceKey<Level> dimension;
    public final BlazeRegistry.Key<MapType> map;
    public final List<BlazeRegistry.Key<Layer>> layers;
    public final TileResolution resolution;
    private volatile int tilesTotal, tilesCurrent;
    private volatile Stage stage = Stage.QUEUED;
    private volatile int flashUntil = 0;
    private volatile boolean errored;

    private final int maxSize;
    private int atlasStartX, atlasStartZ;
    private int atlasEndX, atlasEndZ;
    private int regionsX, regionsZ, grossArea;
    private final List<AtlasPage> pages = new ArrayList<>();
    private final RegionPos core;

    public AtlasTask(ResourceKey<Level> dimension, BlazeRegistry.Key<MapType> map, List<BlazeRegistry.Key<Layer>> layers, TileResolution resolution, RegionPos core) {
        this.dimension = dimension;
        this.map = map;
        this.layers = new ArrayList<>(layers);
        this.resolution = resolution;
        this.core = core;
        this.maxSize = BlazeMapConfig.CLIENT.atlasMaxSize.get() * (TileResolution.FULL.regionSizeKb / resolution.regionSizeKb);
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
        SAVING,
        COMPLETE
    }

    void flash() {
        flashUntil = 3 + (int)(System.currentTimeMillis() / 1000L);
    }

    void exportAsync() {
        NativeImage image = null;

        try {
            // Calculation stage: figure out image size and corner regions, create canvas of appropriate size
            setStage(Stage.CALCULATING);
            StorageAccess.Internal storage = BlazeMapClientEngine.getDimensionStorage(dimension);
            final NativeImage atlas = image = constructAtlas(storage, resolution);

            // Stitching stage: open tiles and transfer pixels to the correct place in the atlas
            setStage(Stage.STITCHING);
            final RegionPos origin = new RegionPos(atlasStartX, atlasStartZ);
            for(var layerKey : map.value().getLayers()) { // Loop layers
                if(!layers.contains(layerKey)) continue;

                File folder = storage.getMipmap(layerKey.location, ".", resolution);
                pages.forEach(page -> renderAtlasPage(page, folder, atlas, origin));
            }

            // Saving stage: flush atlas to disk
            setStage(Stage.SAVING);
            File file = getExportFile();
            file.getParentFile().mkdirs();
            image.writeToFile(file);

            // Completed Stage: Send the filepath into chat
            setStage(Stage.COMPLETE);
            Component filepath = (new TextComponent(file.getPath())).withStyle(ChatFormatting.UNDERLINE).withStyle((style) -> {
                return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
            });
            Component message = Helpers.translate("blazemap.gui.worldmap.atlas_progress.filepath_message", filepath);
            Helpers.getPlayer().sendMessage(message, Util.NIL_UUID);
        }
        catch(Exception e) {
            BlazeMap.LOGGER.error("Error in AtlasExporter", e);
            errored = true;
            try {Thread.sleep(2500);}
            catch(InterruptedException ignored) {}
        }
        finally {
            AtlasExporter.resetTask();
            Helpers.closeQuietly(image);
        }
    }

    private void renderAtlasPage(AtlasPage page, File folder, NativeImage atlas, RegionPos origin) {
        page.forEach(region -> {
            File file = new File(folder, LayerRegionTile.getImageName(region));
            if(!file.exists()) return;

            // non-atomic op on volatile int is ok because only 1 thread writes to variable
            // Java guarantees r/w access to 32-bit variables is atomic, so other threads will read either old or new value with no need for synchronization and no risk of corruption.
            tilesCurrent++;

            try(NativeImage tile = readTile(file)) {
                if(tile == null) return;
                int width = tile.getWidth(), height = tile.getHeight();
                if(width != resolution.regionWidth || height != resolution.regionWidth) {
                    BlazeMap.LOGGER.error("Tile {} ({} x {}) mismatches expectation ({} x {}), skipping", file, width, height, resolution.regionWidth, resolution.regionWidth);
                    return;
                }

                int regionOffsetX = (region.x - origin.x) * resolution.regionWidth;
                int regionOffsetZ = (region.z - origin.z) * resolution.regionWidth;

                for(int x = 0; x < resolution.regionWidth; x++) { // Loop pixels
                    for(int z = 0; z < resolution.regionWidth; z++) {
                        int atlasPixelX = regionOffsetX + x;
                        int atlasPixelZ = regionOffsetZ + z;
                        int atlasPixel = atlas.getPixelRGBA(atlasPixelX, atlasPixelZ);
                        int tilePixel = tile.getPixelRGBA(x, z);
                        atlas.setPixelRGBA(atlasPixelX, atlasPixelZ, Colors.layerBlend(atlasPixel, tilePixel));
                    }
                }
            }
        });
    }

    /**
     * Tries to open a tile.
     * If it fails, waits 250ms and tries again.
     * The 5th failed attempt will abort and throw an IOException.
     */
    private static NativeImage readTile(File file) {
        int attempt = 1;
        while(true) {
            try {
                return NativeImage.read(Files.newInputStream(file.toPath()));
            }
            catch(Exception e) {
                if(attempt < 5) { // we start counting at 1, so 5th attempt throws
                    BlazeMap.LOGGER.warn(String.format("Failed to open file \"%s\" %d times, retrying", file, attempt), e);
                    try {Thread.sleep(250);}
                    catch(InterruptedException ignored) {}
                    attempt++;
                }
                else {
                    BlazeMap.LOGGER.error(String.format("Failed to open file \"%s\" %d times, aborting", file, attempt), e);
                    return null;
                }
            }
        }
    }

    private NativeImage constructAtlas(StorageAccess.Internal storage, TileResolution resolution) {
        Set<RegionPos> regions = new HashSet<>();
        var layers = map.value().getLayers();

        // Determine which regions of the map will need to be rendered
        for(var layer : layers) {
            if(!layers.contains(layer)) continue;
            File folder = storage.getMipmap(layer.location, ".", resolution);
            File[] images = folder.listFiles();
            if(images == null) continue;

            for(var image : images) {
                String filename = image.getName();
                if(!filename.endsWith(".png")) continue; // skip buffers
                String[] coords = filename.replaceAll("(^\\[)|(]\\.png$)", "").split(",");
                if(coords.length != 2) continue;
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                regions.add(new RegionPos(x, z));

                // non-atomic op on volatile int is ok because only 1 thread writes to variable
                // Java guarantees r/w access to 32-bit variables is atomic, so other threads will read either old or new value with no need for synchronization and no risk of corruption.
                tilesTotal++;
            }
        }

        paginateRegions(regions);
        minimizeMemoryFootprint();

        return new NativeImage(NativeImage.Format.RGBA, resolution.regionWidth * regionsX, resolution.regionWidth * regionsZ, true);
    }

    /** Employ strategies to minimize the size of giant maps */
    private void minimizeMemoryFootprint() {
        // Drop far away atlas pages until satisfied
        while(grossArea > maxSize && pages.size() > 1) {
            int maxDistance = -1;
            AtlasPage furthest = null;

            for(var page : pages) {
                int coreDistance = page.getCenterOfMass().distanceSquared(core);
                if(coreDistance > maxDistance) {
                    maxDistance = coreDistance;
                    furthest = page;
                }
            }

            pages.remove(furthest);
            calculateAtlasSize();
        }

        if(grossArea <= maxSize) return;

        // If size still too big, shrink last page as needed
        AtlasPage lastPage = pages.get(0);
        while(lastPage.getGrossArea() > maxSize) {
            lastPage.shrink(core);
        }

        calculateAtlasSize();
    }

    /** Cluster regions into "pages" based on adjacency */
    private void paginateRegions(Set<RegionPos> regions) {
        List<AtlasPage> adjacent = new ArrayList<>();

        for(var region : regions) {
            adjacent.clear();

            for(var page : pages) {
                if(page.isAdjacent(region)) {
                    adjacent.add(page);
                }
            }

            switch(adjacent.size()) {
                case 0 -> pages.add(new AtlasPage(region)); // no adjacent clusters, make new cluster of 1 region
                case 1 -> adjacent.get(0).add(region); // one adjacent cluster, join it
                default -> { // multiple adjacent clusters, merge everything
                    pages.removeAll(adjacent);
                    pages.add(new AtlasPage(region, adjacent));
                }
            }
        }

        for(var page : pages) {
            page.calculateSize();
        }

        calculateAtlasSize();
    }

    private void calculateAtlasSize() {
        atlasStartX = atlasStartZ = Integer.MAX_VALUE;
        atlasEndX = atlasEndZ = Integer.MIN_VALUE;

        for(var page : pages) {
            int startX = page.getStartX(), startZ = page.getStartZ();
            int endX = page.getEndX(), endZ = page.getEndZ();

            if(startX < atlasStartX) atlasStartX = startX;
            if(startZ < atlasStartZ) atlasStartZ = startZ;
            if(endX > atlasEndX) atlasEndX = endX;
            if(endZ > atlasEndZ) atlasEndZ = endZ;
        }

        regionsX = 1 + atlasEndX - atlasStartX;
        regionsZ = 1 + atlasEndZ - atlasStartZ;
        grossArea = regionsX * regionsZ;
    }

    private static File getExportFile() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // fuck you too Java
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return new File(Minecraft.getInstance().gameDirectory, String.format("screenshots/%04d-%02d-%02d_%02d.%02d.%02d-blazemap-export.png", year, month, day, hour, minute, second));
    }
}
