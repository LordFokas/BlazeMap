package com.eerussianguy.blazemap.integration;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.debug.ModAnnouncementEvent;

public class KnownMods {
    private static final HashMap<String, ModInfo> CORE = new HashMap<>();
    private static final HashMap<String, ModInfo> COMPAT = new HashMap<>();
    private static final HashMap<String, ModInfo> PROBLEM = new HashMap<>();
    private static final HashMap<String, ModInfo> API_CALL = new HashMap<>();
    private static final HashMap<String, ModInfo> ANNOUNCED = new HashMap<>();
    private static final HashSet<String> ALL_KNOWN = new HashSet<>();

    public static final String UNKNOWN_VERSION = "0.0NONE";
    public static final String DEVELOPMENT_VERSION = "(dev)";

    public static void init() {
        add(CORE, ModIDs.MINECRAFT);
        add(CORE, ModIDs.FORGE);
        add(CORE, BlazeMap.MOD_ID);

        add(COMPAT, ModIDs.SODIUM);
        add(COMPAT, ModIDs.EMBEDDIUM);
        add(COMPAT, ModIDs.RUBIDIUM);

        add(PROBLEM, ModIDs.OPTIFINE);
        add(PROBLEM, ModIDs.FANCY_MENU);

        HashSet<String> mods = new HashSet<>();
        MinecraftForge.EVENT_BUS.post(new ModAnnouncementEvent(mods));

        for(var integration : BlazeMap.INTEGRATIONS) {
            if(!integration.enabled()) continue;
            mods.addAll(Arrays.asList(integration.dependencies));
        }

        for(String mod : mods){
            add(ANNOUNCED, mod);
        }
    }

    @SafeVarargs
    public static boolean isAnyLoaded(Iterable<String> ... lists) {
        for(var list : lists){
            for(var key : list){
                if(ALL_KNOWN.contains(key)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isLoaded(String modID) {
        return ModList.get().getModContainerById(modID).isPresent();
    }

    public static String getOwnerName(BlazeRegistry.Key<?> key) {
        return API_CALL.get(key.location.getNamespace()).name;
    }

    public static String getOwnerName(ResourceLocation key) {
        return API_CALL.get(key.getNamespace()).name;
    }

    @SafeVarargs
    public static <T> T[] getCore(Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        return mapEntries(CORE, t, function, fallbacks);
    }

    @SafeVarargs
    public static <T> T[] getCompat(Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        return mapEntries(COMPAT, t, function, fallbacks);
    }

    @SafeVarargs
    public static <T> T[] getProblem(Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        return mapEntries(PROBLEM, t, function, fallbacks);
    }

    @SafeVarargs
    public static <T> T[] getAPICall(Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        return mapEntries(API_CALL, t, function, fallbacks);
    }

    @SafeVarargs
    public static <T> T[] getAnnounced(Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        return mapEntries(ANNOUNCED, t, function, fallbacks);
    }

    @SafeVarargs
    private static <T> T[] mapEntries(HashMap<String, ModInfo> map, Class<T> t, Function<ModInfo, ? extends T> function, T ... fallbacks){
        if(map.size() == 0){
            return fallbacks;
        }else{
            return map.values().stream().map(function).collect(Collectors.toList()).toArray((T[]) Array.newInstance(t, map.size()));
        }
    }

    public static void addIntegration(ModIntegration integration) {
        add(COMPAT, integration.modID);
    }

    public static void addRegistry(BlazeRegistry<?> registry){
        for(var key : registry.keys()){
            String mod = key.location.getNamespace();
            add(API_CALL, mod);
        }
    }

    private static void add(HashMap<String, ModInfo> map, String modId){
        var container = ModList.get().getModContainerById(modId);
        if(container.isEmpty()) return;
        map.put(modId, new ModInfo(container.get()));
        ALL_KNOWN.add(modId);
    }

    public static class ModInfo {
        public final String id, name, version;

        public ModInfo(ModContainer container){
            this.id = container.getModId();
            var info = container.getModInfo();
            this.name = info.getDisplayName();
            this.version = coalesce(info.getVersion().toString());
        }

        private static String coalesce(String version) {
            if(!FMLEnvironment.production && version.equals(UNKNOWN_VERSION)) {
                return DEVELOPMENT_VERSION;
            }
            return version;
        }
    }
}
