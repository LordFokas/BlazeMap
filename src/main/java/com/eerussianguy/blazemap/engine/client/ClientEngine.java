package com.eerussianguy.blazemap.engine.client;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.event.DimensionChangedEvent;
import com.eerussianguy.blazemap.api.event.ServerJoinedEvent;
import com.eerussianguy.blazemap.api.maps.LayerRegion;
import com.eerussianguy.blazemap.api.markers.MarkerStorage;
import com.eerussianguy.blazemap.api.pipeline.MasterDatum;
import com.eerussianguy.blazemap.api.pipeline.PipelineType;
import com.eerussianguy.blazemap.api.util.StorageAccess;
import com.eerussianguy.blazemap.config.BlazeMapConfig;
import com.eerussianguy.blazemap.engine.BlazeMapAsync;
import com.eerussianguy.blazemap.engine.RegistryController;
import com.eerussianguy.blazemap.engine.storage.InternalStorage;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCache;
import com.eerussianguy.blazemap.engine.storage.StorageType;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.network.BlazeNetwork;

public class ClientEngine {
    private static final Set<Consumer<LayerRegion>> TILE_CHANGE_LISTENERS = new HashSet<>();
    private static final Map<ResourceKey<Level>, ClientPipeline> PIPELINES = new HashMap<>();

    private static ClientPipeline activePipeline;
    private static MarkerStorage.MapComponentStorage activeLabels;
    private static File baseDir;
    private static String serverID;
    private static InternalStorage storage;
    private static boolean isServerSource;
    private static String mdSource;

    public static void init() {
        BlazeNetwork.initEngine();
        MinecraftForge.EVENT_BUS.register(ClientEngine.class);
    }

    public static InternalStorage getDimensionStorage(ResourceKey<Level> dimension) {
        return storage.internal(dimension.location());
    }

    @SubscribeEvent
    public static void onJoinServer(ClientPlayerNetworkEvent.LoggedInEvent event) {
        RegistryController.ensureRegistriesReady();
        LocalPlayer player = event.getPlayer();
        if(player == null) return;
        serverID = Helpers.getServerID();
        storage = new InternalStorage(StorageType.SERVER, getStorageDir());
        isServerSource = detectRemote(event.getConnection());
        ServerJoinedEvent serverJoined = new ServerJoinedEvent(serverID, storage.addon(), isServerSource);
        MinecraftForge.EVENT_BUS.post(serverJoined);
        switchToPipeline(player.level.dimension());
        mdSource = "unknown";
    }

    private static File getStorageDir() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.hasSingleplayerServer()) {
            return new File(mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile(), "blazemap-client");
        }
        else {
            if(baseDir == null) baseDir = new File(mc.gameDirectory, "blazemap-servers");
            return new File(baseDir, mc.getCurrentServer().ip.replace(':', '+'));
        }
    }

    private static boolean detectRemote(Connection connection) {
        if(Helpers.isIntegratedServerRunning()) {
            return BlazeMapConfig.COMMON.enableServerEngine.get();
        } else {
            return BlazeNetwork.engine().isRemotePresent(connection);
        }
    }

    @SubscribeEvent
    public static void onLeaveServer(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        PIPELINES.clear();
        if(activePipeline != null) {
            activePipeline.shutdown();
            activePipeline = null;
        }
        activeLabels = null;
        serverID = null;
        storage = null;
    }

    @SubscribeEvent
    public static void onChangeWorld(PlayerEvent.PlayerChangedDimensionEvent event) {
        switchToPipeline(event.getTo());
    }

    private static void switchToPipeline(ResourceKey<Level> dimension) {
        if(activePipeline != null) {
            if(activePipeline.dimension.equals(dimension)) return;
            activePipeline.shutdown();
        }
        activePipeline = getPipeline(dimension);
        activeLabels = new LabelStorage(dimension);

        StorageAccess fileStorage = activePipeline.addonStorage;

        TILE_CHANGE_LISTENERS.clear();
        DimensionChangedEvent event = new DimensionChangedEvent(
            dimension,
            activePipeline.availableMapTypes,
            activePipeline.availableLayers,
            TILE_CHANGE_LISTENERS::add,
            activePipeline::consumeTile,
            activeLabels,
            fileStorage
        );
        MinecraftForge.EVENT_BUS.post(event);
    }

    private static ClientPipeline getPipeline(ResourceKey<Level> dimension) {
        BlazeMapAsync async = BlazeMapAsync.instance();
        return PIPELINES.computeIfAbsent(dimension, d -> new ClientPipeline(async.clientChain, async.debouncer, d, storage.internal(d.location()), isClientSource() ? PipelineType.CLIENT_STANDALONE : PipelineType.CLIENT_AND_SERVER)).activate();
    }

    public static ChunkMDCache getMDCache(ChunkPos pos) {
        if(activePipeline == null || !activePipeline.isMDCached()) return null;
        return activePipeline.getMDCache().getChunkCache(pos);
    }

    public static void forceRedrawFromMD(ChunkPos pos) {
        if(activePipeline == null) return;
        BlazeMapAsync.instance().clientChain.runOnDataThread(() -> activePipeline.redrawFromMD(pos));
    }

    public static void onChunkChanged(ChunkPos pos, String source) {
        if(isServerSource) {
            if(activePipeline != null) {
                activePipeline.setHot();
            }
            return;
        }
        if(activePipeline == null) {
            BlazeMap.LOGGER.warn("Ignoring chunk update for {}, pipeline: {}, isServerSource: {}, brand: {}", pos, activePipeline.getClass().getSimpleName() + "@" + activePipeline.hashCode(), isServerSource, source);
            return;
        }
        mdSource = source;
        activePipeline.onChunkChanged(pos);
    }

    public static void submitChanges(ResourceKey<Level> dimension, ChunkPos pos, List<MasterDatum> data, String source) {
        isServerSource = true;
        mdSource = source;
        getPipeline(dimension).insertMasterData(pos, data);
    }

    static void notifyLayerRegionChange(LayerRegion layerRegion) {
        for(Consumer<LayerRegion> listener : TILE_CHANGE_LISTENERS) {
            listener.accept(layerRegion);
        }
    }


    // =================================================================================================================
    // Debug Info Access
    public static String getMDSource() {
        return mdSource;
    }

    public static boolean isClientSource() {
        return !isServerSource;
    }

    public static int numCollectors() {
        return activePipeline.numCollectors;
    }

    public static int numProcessors() {
        return activePipeline.numProcessors;
    }

    public static int numTransformers() {
        return activePipeline.numTransformers;
    }

    public static int numLayers() {
        return activePipeline.numLayers;
    }

    public static int dirtyTiles() {
        return activePipeline.getDirtyTiles();
    }

    public static int dirtyChunks() {
        return activePipeline.getDirtyChunks();
    }

    public static String avgFPS(){
        return Minecraft.getInstance().fpsString.split(" ")[0];
    }
}
