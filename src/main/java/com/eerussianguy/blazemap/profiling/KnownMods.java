package com.eerussianguy.blazemap.profiling;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapAPI;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.debug.ModAnnouncementEvent;
import com.eerussianguy.blazemap.feature.ModIntegration.ModIDs;

public class KnownMods {
    private static final HashMap<String, ModInfo> CORE = new HashMap<>();
    private static final HashMap<String, ModInfo> COMPAT = new HashMap<>();
    private static final HashMap<String, ModInfo> PROBLEM = new HashMap<>();
    private static final HashMap<String, ModInfo> APICALL = new HashMap<>();
    private static final HashMap<String, ModInfo> ANNOUNCED = new HashMap<>();
    private static final HashSet<String> ALL_KNOWN = new HashSet<>();

    public static void init() {
        add(CORE, ModIDs.MINECRAFT);
        add(CORE, ModIDs.FORGE);
        add(CORE, BlazeMap.MOD_ID);

        add(COMPAT, ModIDs.SODIUM);
        add(COMPAT, ModIDs.EMBEDDIUM);
        add(COMPAT, ModIDs.RUBIDIUM);

        add(PROBLEM, ModIDs.OPTIFINE);
        add(PROBLEM, ModIDs.FANCY_MENU);

        add(BlazeMapAPI.MASTER_DATA);
        add(BlazeMapAPI.COLLECTORS);
        add(BlazeMapAPI.TRANSFORMERS);
        add(BlazeMapAPI.PROCESSORS);
        add(BlazeMapAPI.LAYERS);
        add(BlazeMapAPI.MAPTYPES);
        add(BlazeMapAPI.OBJECT_RENDERERS);

        HashSet<String> mods = new HashSet<>();
        MinecraftForge.EVENT_BUS.post(new ModAnnouncementEvent(mods));
        for(String mod : mods){
            add(ANNOUNCED, mod);
        }

        ALL_KNOWN.addAll(CORE.keySet());
        ALL_KNOWN.addAll(COMPAT.keySet());
        ALL_KNOWN.addAll(PROBLEM.keySet());
        ALL_KNOWN.addAll(APICALL.keySet());
        ALL_KNOWN.addAll(ANNOUNCED.keySet());
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
        return mapEntries(APICALL, t, function, fallbacks);
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

    private static void add(BlazeRegistry<?> registry){
        for(var key : registry.keys()){
            String mod = key.location.getNamespace();
            APICALL.computeIfAbsent(mod, id -> new ModInfo(ModList.get().getModContainerById(id).get()));
        }
    }

    private static void add(HashMap<String, ModInfo> map, String modId){
        var container = ModList.get().getModContainerById(modId);
        container.ifPresent(mod -> map.put(modId, new ModInfo(mod)));
    }

    public static class ModInfo {
        public final String id, name, version;

        public ModInfo(ModContainer container){
            this.id = container.getModId();
            var info = container.getModInfo();
            this.name = info.getDisplayName();
            this.version = info.getVersion().toString();
        }
    }
}
