package com.eerussianguy.blazemap.api.event;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.maps.Layer;

public class MapMenuSetupEvent extends Event {
    public final MenuFolder root;
    public final List<BlazeRegistry.Key<Layer>> layers;
    public final int blockPosX, blockPosZ;
    public final int chunkPosX, chunkPosZ;
    public final int regionPosX, regionPosZ;

    public MapMenuSetupEvent(MenuFolder root, List<BlazeRegistry.Key<Layer>> layers, int blockPosX, int blockPosZ, int chunkPosX, int chunkPosZ, int regionPosX, int regionPosZ) {
        this.root = root;
        this.layers = Collections.unmodifiableList(layers);
        this.blockPosX = blockPosX;
        this.blockPosZ = blockPosZ;
        this.chunkPosX = chunkPosX;
        this.chunkPosZ = chunkPosZ;
        this.regionPosX = regionPosX;
        this.regionPosZ = regionPosZ;
    }


    public static abstract class MenuItem {
        public final ResourceLocation id; // See BlazeMapReferences.MapMenu
        public final ResourceLocation icon;
        public final Component text;

        private MenuItem(ResourceLocation id, ResourceLocation icon, Component text) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            this.icon = icon;
            this.text = Objects.requireNonNull(text, "text must not be null");
        }
    }

    public static class MenuAction extends MenuItem {
        public final Runnable function;

        public MenuAction(ResourceLocation id, ResourceLocation icon, Component text, Runnable action) {
            super(id, icon, text);
            this.function = Objects.requireNonNull(action, "action must not be null");
        }
    }

    public static class MenuFolder extends MenuItem {
        private final LinkedList<MenuItem> children = new LinkedList<>();

        public MenuFolder(ResourceLocation id, ResourceLocation icon, Component text, MenuItem ... children) {
            super(id, icon, text);
            for(MenuItem item : children) {
                this.add(item);
            }
        }

        public void add(MenuItem item) {
            this.children.add(Objects.requireNonNull(item, "child item must not be null"));
        }

        public int size() {
            return this.children.size();
        }

        public void consume(Consumer<MenuItem> consumer) {
            children.forEach(consumer);
        }
    }
}
