package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.DimensionChangedEvent;
import com.eerussianguy.blazemap.api.event.MapLabelEvent;
import com.eerussianguy.blazemap.api.maps.*;
import com.eerussianguy.blazemap.api.markers.*;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.async.AsyncAwaiter;
import com.eerussianguy.blazemap.feature.BlazeMapFeaturesClient;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.profiling.Profiler;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

public class MapRenderer implements AutoCloseable {
    private static final ResourceLocation PLAYER = Helpers.identifier("textures/player.png");
    private static final List<MapRenderer> RENDERERS = new ArrayList<>(4);
    private static DimensionTileStorage tileStorage;
    private static ResourceKey<Level> dimension;
    private static MarkerStorage.MapComponentStorage labelStorage;

    public static void onDimensionChange(DimensionChangedEvent evt) {
        evt.tileNotifications.addUpdateListener(MapRenderer::onTileChanged);
        tileStorage = evt.tileStorage;
        dimension = evt.dimension;
        labelStorage = evt.labels;
    }

    public static void onMapLabelAdded(MapLabelEvent.Created event) {
        RENDERERS.forEach(r -> r.add(event.label));
    }

    public static void onMapLabelRemoved(MapLabelEvent.Removed event) {
        RENDERERS.forEach(r -> r.remove(event.label));
    }

    private static void onTileChanged(LayerRegion tile) {
        RENDERERS.forEach(r -> r.onLayerChanged(tile.layer, tile.region));
    }


    // =================================================================================================================


    final DebugInfo debug = new DebugInfo();
    private Profiler.TimeProfiler renderTimer = new Profiler.TimeProfiler.Dummy();
    private Profiler.TimeProfiler uploadTimer = new Profiler.TimeProfiler.Dummy();

    private MapType mapType;
    private List<BlazeRegistry.Key<Layer>> layers_on, layers_off;
    private List<BlazeRegistry.Key<Overlay>> overlays_on, overlays_off = new LinkedList<>();
    private final HashMap<BlazeRegistry.Key<MapType>, List<BlazeRegistry.Key<Layer>>> disabledLayers = new HashMap<>();
    private final List<MapComponentMarker> labels = new ArrayList<>(16);
    private final List<MapComponentMarker> labels_on = new ArrayList<>(16);
    private final List<MapComponentMarker> labels_off = new ArrayList<>(16);
    private boolean hasActiveSearch = false;
    private Predicate<String> matcher;
    private Consumer<Boolean> searchHost;

    private final ResourceLocation textureResource;
    private DynamicTexture mapTexture;
    private RenderType renderType;
    private boolean needsUpdate = true;
    private final Marker<?> playerMarker;

    private int width, height;
    private int mapWidth, mapHeight;
    private final BlockPos.MutableBlockPos center, begin, end;
    private RegionPos[][] offsets;
    private final double minZoom, maxZoom;
    private double zoom = 1;
    private TileResolution resolution;

    public MapRenderer(int width, int height, ResourceLocation textureResource, double minZoom, double maxZoom) {
        this.center = new BlockPos.MutableBlockPos();
        this.begin = new BlockPos.MutableBlockPos();
        this.end = new BlockPos.MutableBlockPos();
        this.textureResource = textureResource;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        resolution = TileResolution.FULL;

        updateVisibleOverlays();
        selectMapType();
        centerOnPlayer();

        if(width > 0 && height > 0) {
            this.resize(width, height);
        }

        LocalPlayer player = Helpers.getPlayer();
        playerMarker = new Marker<>(Helpers.identifier("local_player"), player.level.dimension(), player.blockPosition(), PLAYER).setSize(32).setRotation(player.getRotationVector().y);

        RENDERERS.add(this);
        debug.zoom = zoom;
    }

    private void selectMapType() {
        if(dimension != null && (mapType == null || !mapType.shouldRenderInDimension(dimension))) {
            for(BlazeRegistry.Key<MapType> next : BlazeMapAPI.MAPTYPES.keys()) {
                MapType type = next.value();
                if(type.shouldRenderInDimension(dimension)) {
                    setMapType(type);
                    break;
                }
            }
        }
    }

    public void resize(int width, int height) {
        this.width = debug.rw = width;
        this.height = debug.rh = height;

        selectMapType();
        createImage();
    }

    private void createImage() {
        makeOffsets();
        if(mapTexture != null) {
            mapTexture.close();
        }
        double factor = zoom < 1 ? 1 : zoom;
        resolution = (zoom < 1) ? TileResolution.byZoom(zoom) : TileResolution.FULL;
        mapWidth = (int) (width / factor);
        mapHeight = (int) (height / factor);
        mapTexture = new DynamicTexture(mapWidth, mapHeight, false);
        Minecraft.getInstance().getTextureManager().register(textureResource, mapTexture);
        renderType = RenderType.text(textureResource);
        needsUpdate = true;

        debug.mw = mapWidth * resolution.pixelWidth;
        debug.mh = mapHeight * resolution.pixelWidth;
    }

    private void makeOffsets() {
        this.mapWidth = (int) (width / zoom);
        this.mapHeight = (int) (height / zoom);

        int w2 = mapWidth / 2, h2 = mapHeight / 2;
        RegionPos b = new RegionPos(begin.set(center.offset(-w2, 0, -h2)));
        RegionPos e = new RegionPos(end.set(center.offset(w2, 0, h2)));

        int dx = debug.ox = e.x - b.x + 1;
        int dz = debug.oz = e.z - b.z + 1;

        offsets = new RegionPos[dx][dz];
        for(int x = 0; x < dx; x++) {
            for(int z = 0; z < dz; z++) {
                offsets[x][z] = b.offset(x, z);
            }
        }

        updateLabels();

        // debug info
        debug.bx = begin.getX();
        debug.bz = begin.getZ();
        debug.ex = end.getX();
        debug.ez = end.getZ();
    }

    public void updateLabels() {
        labels.clear();
        layers_on.forEach(layer -> labels.addAll(labelStorage.getStorage(layer).getAll().stream().filter(l -> inRange(l.getPosition())).collect(Collectors.toList())));
        debug.labels = labels.size();
        labels.forEach(this::matchLabel);
        pingSearchHost();
    }

    private void add(MapComponentMarker label) {
        if(inRange(label.getPosition()) && layers_on.contains(label.getComponentId())) {
            labels.add(label);
            debug.labels++;
            matchLabel(label);
            pingSearchHost();
        }
    }

    private void remove(MapComponentMarker label) {
        if(labels.remove(label)) {
            debug.labels--;
            labels_off.remove(label);
            labels_on.remove(label);
            pingSearchHost();
        }
    }

    private void onLayerChanged(BlazeRegistry.Key<Layer> layer, RegionPos region) {
        if(!layers_on.contains(layer)) return;
        RegionPos r0 = offsets[0][0];
        if(r0.x > region.x || r0.z > region.z) return;
        RegionPos[] arr = offsets[offsets.length - 1];
        RegionPos r1 = arr[arr.length - 1];
        if(r1.x < region.x || r1.z < region.z) return;
        needsUpdate = true;
    }

    private void updateVisibleLayers() {
        layers_on = mapType.getLayers().stream().filter(l -> !layers_off.contains(l) && l.value().shouldRenderInDimension(dimension)).collect(Collectors.toList());
        updateLabels();
        debug.layers = layers_on.size();
    }

    private void updateVisibleOverlays() {
        overlays_on = BlazeMapFeaturesClient.OVERLAYS.stream().filter(o -> !overlays_off.contains(o) && o.value().shouldRenderInDimension(dimension)).collect(Collectors.toList());
        debug.overlays = overlays_on.size();
    }

    private boolean inRange(BlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        return x >= begin.getX() && x <= end.getX() && z >= begin.getZ() && z <= end.getZ();
    }


    // =================================================================================================================


    public void render(PoseStack stack, MultiBufferSource buffers) {
        if(needsUpdate) updateTexture();

        stack.pushPose();
        Matrix4f matrix = stack.last().pose();

        RenderHelper.fillRect(buffers, matrix, this.width, this.height, 0xFF333333);
        RenderHelper.drawQuad(buffers.getBuffer(renderType), matrix, width, height);

        stack.pushPose();
        ClientLevel level = Minecraft.getInstance().level;
        for(var key : overlays_on) {
            stack.translate(0, 0, 0.1f);
            stack.pushPose();
            key.value().getMarkers(level, resolution).forEach(marker -> {
                stack.translate(0, 0, 0.0001f);
                renderObject(buffers, stack, marker, SearchTargeting.NONE);
            });
            stack.popPose();
        }
        stack.popPose();

        stack.pushPose();
        if(hasActiveSearch) {
            for(MapComponentMarker l : labels_off) {
                renderObject(buffers, stack, l, SearchTargeting.MISS);
            }
            for(MapComponentMarker l : labels_on) {
                renderObject(buffers, stack, l, SearchTargeting.HIT);
            }
        }
        else {
            for(MapComponentMarker l : labels) {
                renderObject(buffers, stack, l, SearchTargeting.NONE);
            }
        }

        LocalPlayer player = Helpers.getPlayer();
        stack.translate(0, 0, 1);
        playerMarker.setPosition(player.blockPosition()).setRotation(player.getRotationVector().y);
        renderObject(buffers, stack, playerMarker, SearchTargeting.NONE);
        stack.popPose();

        stack.popPose();
    }

    private void updateTexture() {
        NativeImage texture = mapTexture.getPixels();
        if(texture == null) return;
        int textureH = texture.getHeight();
        int textureW = texture.getWidth();
        texture.fillRect(0, 0, textureW, textureH, 0);

        int cornerXOffset = ((begin.getX() % 512) + 512) % 512;
        int cornerZOffset = ((begin.getZ() % 512) + 512) % 512;
        int regionCount = offsets.length * offsets[0].length;

        renderTimer.begin();
        /**
         * Commenting out the following until the thread pool depletion issue has been solved
         */
        // if(regionCount > 6) {
        //     debug.stitching = "Parallel";

        //     AsyncAwaiter jobs = new AsyncAwaiter(regionCount);
        //     for(int regionIndexX = 0; regionIndexX < offsets.length; regionIndexX++) {
        //         for(int regionIndexZ = 0; regionIndexZ < offsets[regionIndexX].length; regionIndexZ++) {
        //             generateMapTileAsync(texture, resolution, textureW, textureH, cornerXOffset, cornerZOffset, regionIndexX, regionIndexZ, jobs);
        //         }
        //     }

        //     jobs.await();
        // }
        // else {
            debug.stitching = "Sequential";

            for(int regionIndexX = 0; regionIndexX < offsets.length; regionIndexX++) {
                for(int regionIndexZ = 0; regionIndexZ < offsets[regionIndexX].length; regionIndexZ++) {
                    generateMapTile(texture, resolution, textureW, textureH, cornerXOffset, cornerZOffset, regionIndexX, regionIndexZ);
                }
            }
        // }
        renderTimer.end();

        uploadTimer.begin();
        mapTexture.upload();
        uploadTimer.end();

        needsUpdate = false;
    }

    // Run generateMapTile in an engine background thread. Useful for parallelizing massive workloads.
    private void generateMapTileAsync(NativeImage texture, TileResolution resolution, int textureW, int textureH, int cornerXOffset, int cornerZOffset, int regionIndexX, int regionIndexZ, AsyncAwaiter jobs) {
        BlazeMapAsync.instance().clientChain.runOnDataThread(() -> {
            generateMapTile(texture, resolution, textureW, textureH, cornerXOffset, cornerZOffset, regionIndexX, regionIndexZ);
            jobs.done();
        });
    }

    private void generateMapTile(NativeImage texture, TileResolution resolution, int textureW, int textureH, int cornerXOffset, int cornerZOffset, int regionIndexX, int regionIndexZ) {
        // Precomputing values so they don't waste CPU cycles recalculating for each pixel
        final RegionPos region = offsets[regionIndexX][regionIndexZ];
        final int cornerXOffsetScaled = cornerXOffset / resolution.pixelWidth;
        final int cornerZOffsetScaled = cornerZOffset / resolution.pixelWidth;
        final int regionFirstPixelX = (regionIndexX * resolution.regionWidth);
        final int regionFirstPixelY = (regionIndexZ * resolution.regionWidth);

        int startX = (region.x * 512) < begin.getX() ? cornerXOffsetScaled : 0;
        int startY = (region.z * 512) < begin.getZ() ? cornerZOffsetScaled : 0;

        if (regionFirstPixelX + startX - cornerXOffsetScaled < 0) {
            // Set x to be the value it should be when textureX == 0
            startX = cornerXOffsetScaled - regionFirstPixelX;
        }
        if (regionFirstPixelY + startY - cornerZOffsetScaled < 0) {
            // Set y to be the value it should be when textureY == 0
            startY = cornerZOffsetScaled - regionFirstPixelY;
        }

        final int _startX = startX, _startY = startY;
        final int textureFirstPixelX = regionFirstPixelX - cornerXOffsetScaled;
        final int textureFirstPixelY = regionFirstPixelY - cornerZOffsetScaled;

        // Paint map layers on the canvas
        for(BlazeRegistry.Key<Layer> layer : layers_on) {
            if(!layer.value().type.isVisible) return;
            tileStorage.consumeTile(layer, region, resolution, source -> transferPixels(texture, source, _startX, _startY, textureFirstPixelX, textureFirstPixelY, textureW, textureH));
        }

        // Paint global overlays on top of the layers
        for(BlazeRegistry.Key<Overlay> overlayKey : overlays_on) {
            Overlay overlay = overlayKey.value();
            if(!overlay.type.isVisible) continue;

            PixelSource source = overlay.getPixelSource(dimension, region, resolution);
            transferPixels(texture, source, _startX, _startY, textureFirstPixelX, textureFirstPixelY, textureW, textureH);
        }
    }

    private void transferPixels(NativeImage texture, PixelSource source, int startX, int startY, int textureFirstPixelX, int textureFirstPixelY, int textureW, int textureH) {
        final int sourceWidth = source.getWidth();
        final int sourceHeight = source.getHeight();

        for(int x = startX; x < sourceWidth; x++) {
            int textureX = textureFirstPixelX + x;

            if(textureX >= textureW) break;

            for(int y = startY; y < sourceHeight; y++) {
                int textureY = textureFirstPixelY + y;

                if(textureY >= textureH) break;

                int color = Colors.layerBlend(texture.getPixelRGBA(textureX, textureY), source.getPixel(x, y));
                texture.setPixelRGBA(textureX, textureY, color);
            }
        }
    }

    private void renderObject(MultiBufferSource buffers, PoseStack stack, Marker<?> marker, SearchTargeting search) {
        if(!inRange(marker.getPosition())) return;

        stack.pushPose();
        stack.scale((float) this.zoom, (float) this.zoom, 1);
        BlockPos position = marker.getPosition();
        int dx = position.getX() - begin.getX();
        int dy = position.getZ() - begin.getZ();
        stack.translate(dx, dy, 0);

        ((ObjectRenderer<Marker<?>>) marker.getRenderer().value()).render(marker, stack, buffers, this.zoom, search);

        stack.popPose();
    }


    // =================================================================================================================


    public void setSearch(String search) {
        labels_off.clear();
        labels_on.clear();
        if(search == null || search.equals("")) {
            hasActiveSearch = false;
            matcher = null;
            return;
        }
        try {
            Pattern pattern = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
            matcher = pattern.asPredicate();
        }
        catch(PatternSyntaxException pse) {
            matcher = (s) -> s.toLowerCase().contains(search.toLowerCase());
        }
        hasActiveSearch = true;
        labels.forEach(this::matchLabel);
    }

    private void matchLabel(MapComponentMarker label) {
        if(!hasActiveSearch) return;
        for(String tag : label.getTags()) {
            if(matcher.test(tag)) {
                labels_on.add(label);
                return;
            }
        }
        labels_off.add(label);
    }

    public void setSearchHost(Consumer<Boolean> searchHost) {
        this.searchHost = searchHost;
    }

    public void pingSearchHost() {
        if(searchHost == null) return;
        searchHost.accept(labels.size() > 0);
    }


    // =================================================================================================================


    public boolean setMapType(MapType mapType) {
        if(this.mapType == mapType || dimension == null) return false;
        if(mapType == null) {
            selectMapType();
            if(this.mapType == mapType) return false;
        }
        else {
            if(!mapType.shouldRenderInDimension(dimension)) return false;
            this.mapType = mapType;
        }
        this.layers_off = disabledLayers.computeIfAbsent(this.mapType.getID(), $ -> new LinkedList<>());
        updateVisibleLayers();
        this.needsUpdate = true;
        return true;
    }

    public MapType getMapType() {
        return mapType;
    }

    List<BlazeRegistry.Key<Layer>> getDisabledLayers() {
        return this.layers_off;
    }

    void setDisabledLayers(List<BlazeRegistry.Key<Layer>> layers) {
        this.layers_off.clear();
        this.layers_off.addAll(layers);
        updateVisibleLayers();
        this.needsUpdate = true;
    }

    List<BlazeRegistry.Key<Overlay>> getDisabledOverlays() {
        return this.overlays_off;
    }

    void setDisabledOverlays(List<BlazeRegistry.Key<Overlay>> overlays) {
        this.overlays_off.clear();
        this.overlays_off.addAll(overlays);
        updateVisibleOverlays();
        this.needsUpdate = true;
    }

    public MapRenderer setProfilers(Profiler.TimeProfiler render, Profiler.TimeProfiler upload) {
        this.renderTimer = render;
        this.uploadTimer = upload;
        return this;
    }

    public boolean setZoom(double zoom) {
        double prevZoom = this.zoom;
        zoom = Helpers.clamp(minZoom, zoom, maxZoom);
        if(prevZoom == zoom) return false;
        this.zoom = debug.zoom = zoom;
        if(width > 0 && height > 0) {
            createImage();
        }
        return true;
    }

    public double getZoom() {
        return zoom;
    }

    public int getBeginX() {
        return begin.getX();
    }

    public int getBeginZ() {
        return begin.getZ();
    }

    public boolean toggleLayer(BlazeRegistry.Key<Layer> layer) {
        if(!mapType.getLayers().contains(layer)) return false;
        if(layers_off.contains(layer)) layers_off.remove(layer);
        else layers_off.add(layer);
        updateVisibleLayers();
        needsUpdate = true;
        return true;
    }

    List<BlazeRegistry.Key<Layer>> getVisibleLayers() {
        return layers_on;
    }

    public boolean isLayerVisible(BlazeRegistry.Key<Layer> layer) {
        return !layers_off.contains(layer);
    }

    public boolean toggleOverlay(BlazeRegistry.Key<Overlay> overlay) {
        if(!overlay.value().shouldRenderInDimension(dimension)) return false;
        if(overlays_off.contains(overlay)) overlays_off.remove(overlay);
        else overlays_off.add(overlay);
        updateVisibleOverlays();
        needsUpdate = true;
        return true;
    }

    List<BlazeRegistry.Key<Overlay>> getVisibleOverlays() {
        return overlays_on;
    }

    public boolean isOverlayVisible(BlazeRegistry.Key<Overlay> overlay) {
        return !overlays_off.contains(overlay);
    }

    public void setCenter(int x, int z) {
        this.center.set(x, 0, z);
        makeOffsets();
        needsUpdate = true;
    }

    public void moveCenter(int x, int z) {
        setCenter(center.getX() + x, center.getZ() + z);
    }

    public RegionPos getCenterRegion() {
        return new RegionPos(center);
    }

    public void centerOnPlayer() {
        LocalPlayer player = Helpers.getPlayer();
        if(player == null) {
            BlazeMap.LOGGER.warn("Ignoring request to center on player because LocalPlayer is null");
            return;
        }
        Vec3 pos = player.position();
        setCenter((int) pos.x, (int) pos.z);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void close() {
        mapTexture.close();
        RENDERERS.remove(this);
    }

    static class DebugInfo {
        int rw, rh, mw, mh;
        int bx, bz, ex, ez;
        double zoom;
        int ox, oz;
        int layers, overlays, labels, waypoints;
        String stitching;
    }
}
