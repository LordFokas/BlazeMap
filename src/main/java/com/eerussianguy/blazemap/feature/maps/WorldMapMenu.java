package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent.MenuAction;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent.MenuFolder;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent.MenuItem;
import com.eerussianguy.blazemap.engine.cache.ChunkMDCache;
import com.eerussianguy.blazemap.engine.client.ClientEngine;
import com.eerussianguy.blazemap.feature.waypoints.WaypointEditorFragment;
import com.eerussianguy.blazemap.feature.waypoints.WaypointService;
import com.eerussianguy.blazemap.feature.waypoints.WaypointSharing;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;

public class WorldMapMenu {
    private static final String BASE_PATH = "worldmap.menu.";
    private static final String BASE_LANG = "blazemap.gui.worldmap.menu.";
    private static final ResourceLocation BLAZE_POWDER = new ResourceLocation("minecraft", "textures/item/blaze_powder.png");

    private static final ResourceLocation MENU_NOOP = BlazeMap.resource("map.menu.noop");
    private static final TranslatableComponent NOOP_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.no_options");
    public static final MapMenuSetupEvent.MenuAction NOOP = new MapMenuSetupEvent.MenuAction(MENU_NOOP, null, NOOP_TEXT, null);

    public static MenuFolder waypoints(int blockX, int blockZ) {
        WaypointService waypoints = WaypointService.instance();

        BlockPos local = new BlockPos(blockX, 0, blockZ);
        MenuFolder folder = makeFolder("waypoint", BlazeMapReferences.Icons.WAYPOINT, Colors.WHITE,
            makeAction("waypoint.new", BlazeMapReferences.Icons.WAYPOINT,
                () -> new WaypointEditorFragment(local).open()
            )
        );
        waypoints.getAll().stream()
            .filter(waypoint -> waypoint.getPosition().atY(0).distSqr(local) < 48)
            .map(waypoint -> makeFolder("waypoint.options", waypoint.getIcon(), waypoint.getColor(), waypoint.getName(),
                    makeAction("waypoint.edit", null, () -> new WaypointEditorFragment(waypoint).open()),
                    makeAction("waypoint.hide", null, null),
                    makeAction("waypoint.share", null, () -> WaypointSharing.shareWaypoint(waypoint)),
                    makeAction("waypoint.delete", null, () -> waypoints.remove(waypoint))
                )
            )
            .forEach(folder::add);
        return folder;
    }

    public static MenuFolder debug(int blockX, int blockZ, int chunkX, int chunkZ, int regionX, int regionZ) {
        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        MenuFolder folder = makeFolder("debug", BLAZE_POWDER, -1,
            makeAction("debug.redraw_chunk_md", null, () -> ClientEngine.forceRedrawFromMD(chunkPos))
        );

        if(!FMLEnvironment.production) {
            ChunkMDCache mdCache = ClientEngine.getMDCache(chunkPos);
            MenuFolder mdInspector = makeFolder("debug.inspect_chunk_md", null, -1);
            if(mdCache != null) {
                mdCache.data().forEach(md -> {
                    ResourceLocation key = md.getID().location;
                    mdInspector.add(
                        makeAction("debug.inspect_chunk_md."+key.getNamespace()+"."+key.getPath(), null, new TextComponent(key.toString()),
                            () -> WorldMapGui.apply(gui -> gui.addInspector(new MDInspectorWidget<>(md, chunkPos)))
                        )
                    );
                });
            }
            folder.add(mdInspector);
        }

        return folder;
    }

    private static MenuAction makeAction(String id, ResourceLocation icon, Runnable function) {
        return new MenuAction(BlazeMap.resource(BASE_PATH + id), icon, Helpers.translate(BASE_LANG + id), function);
    }

    private static MenuAction makeAction(String id, ResourceLocation icon, Component name, Runnable function) {
        return new MenuAction(BlazeMap.resource(BASE_PATH + id), icon, name, function);
    }

    private static MenuFolder makeFolder(String id, ResourceLocation icon, int tint, String name, MenuItem ... children){
        return new MenuFolder(BlazeMap.resource(BASE_PATH + id), icon, tint, new TextComponent(name), children);
    }

    private static MenuFolder makeFolder(String id, ResourceLocation icon, int tint, MenuItem ... children){
        return new MenuFolder(BlazeMap.resource(BASE_PATH + id), icon, tint, Helpers.translate(BASE_LANG + id), children);
    }
}
