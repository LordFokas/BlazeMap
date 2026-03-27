package com.eerussianguy.blazemap.feature.overlays;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.GhostOverlay;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.markers.Marker;
import com.eerussianguy.blazemap.lib.Helpers;

public abstract class EntityOverlay extends GhostOverlay {
    private static final ResourceLocation PLAYER = BlazeMap.resource("textures/player.png");
    private static final ArrayList<Entity> ENTITIES = new ArrayList<>();
    private static final int DEFAULT_ENTITY_MAX_VERTICAL_DISTANCE = 10;
    private static final int DEFAULT_PLAYER_MAX_VERTICAL_DISTANCE = -1;
    private final int maxVerticalDistance;

    protected EntityOverlay(BlazeRegistry.Key<Overlay> id, TranslatableComponent name, ResourceLocation icon, int maxVerticalDistance) {
        super(id, name, icon);
        this.maxVerticalDistance = maxVerticalDistance;
    }

    protected abstract boolean predicate(Entity entity);
    protected abstract int getEntityColor(Entity e);

    protected String getEntityName(Entity e) {
        return null;
    }

    @Override
    public List<Marker<?>> getMarkers(ClientLevel level, TileResolution resolution) {
        ENTITIES.clear();
        LocalPlayer player = Helpers.getPlayer();

        for(var entity : level.entitiesForRendering()) {
            if(predicate(entity) && (maxVerticalDistance < 0 || Math.abs(player.position().y - entity.position().y) <= maxVerticalDistance)) {
                ENTITIES.add(entity);
            }
        }

        return ENTITIES.stream().map(e -> new EntityMarker(e, getEntityName(e), getEntityColor(e))).collect(Collectors.toList());
    }

    private static class EntityMarker extends Marker<EntityMarker> {
        protected EntityMarker(Entity e, String name, int color) {
            super(BlazeMap.resource("entity."+e.getStringUUID()), e.level.dimension(), e.blockPosition(), PLAYER);
            setName(name);
            setColor(color);
            setRotation(e.getRotationVector().y);
        }
    }

    public static class Players extends EntityOverlay {
        public Players() {
            super(
                BlazeMapReferences.Overlays.PLAYERS,
                Helpers.translate("blazemap.players"),
                BlazeMap.resource("textures/map_icons/overlay_players.png"),
                DEFAULT_PLAYER_MAX_VERTICAL_DISTANCE
            );
        }

        @Override
        protected boolean predicate(Entity entity) {
            return entity instanceof Player && entity != Helpers.getPlayer();
        }

        @Override
        protected String getEntityName(Entity e) {
            return e.getName().getString();
        }

        @Override
        protected int getEntityColor(Entity e) {
            return 0xFF88FF66;
        }
    }

    public static class NPCs extends EntityOverlay {
        public NPCs() {
            super(
                BlazeMapReferences.Overlays.NPCS,
                Helpers.translate("blazemap.npcs"),
                BlazeMap.resource("textures/map_icons/overlay_npcs.png"),
                DEFAULT_ENTITY_MAX_VERTICAL_DISTANCE
            );
        }

        @Override
        protected boolean predicate(Entity entity) {
            return entity instanceof AbstractVillager;
        }

        @Override
        protected int getEntityColor(Entity e) {
            return 0xFFFFFF3F;
        }
    }

    public static class Animals extends EntityOverlay {
        public Animals() {
            super(
                BlazeMapReferences.Overlays.ANIMALS,
                Helpers.translate("blazemap.animals"),
                BlazeMap.resource("textures/map_icons/overlay_animals.png"),
                DEFAULT_ENTITY_MAX_VERTICAL_DISTANCE
            );
        }

        @Override
        protected boolean predicate(Entity entity) {
            return entity instanceof Animal || entity instanceof WaterAnimal;
        }

        @Override
        protected int getEntityColor(Entity e) {
            return e instanceof WaterAnimal ? 0xFF4488FF : 0xFFA0A0A0;
        }
    }

    public static class Enemies extends EntityOverlay {
        public Enemies() {
            super(
                BlazeMapReferences.Overlays.ENEMIES,
                Helpers.translate("blazemap.enemies"),
                BlazeMap.resource("textures/map_icons/overlay_enemies.png"),
                DEFAULT_ENTITY_MAX_VERTICAL_DISTANCE
            );
        }

        @Override
        protected boolean predicate(Entity entity) {
            return entity instanceof Monster;
        }

        @Override
        protected int getEntityColor(Entity e) {
            return 0xFFFF2222;
        }
    }
}
