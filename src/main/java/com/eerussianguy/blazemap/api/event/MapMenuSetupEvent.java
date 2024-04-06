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
        public final ResourceLocation id;
        public final ResourceLocation icon;
        public final int iconColor;
        public final Component text;

        private MenuItem(ResourceLocation id, ResourceLocation icon, int color, Component text) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            this.icon = icon;
            this.iconColor = color;
            this.text = Objects.requireNonNull(text, "text must not be null");
        }

        private MenuItem(ResourceLocation id, ResourceLocation icon, Component text) {
            this(id, icon, -1, text);
        }

        private MenuItem(ResourceLocation id, Component text) {
            this(id, null, -1, text);
        }
    }

    public static class MenuAction extends MenuItem {
        public final Runnable function;

        public MenuAction(ResourceLocation id, ResourceLocation icon, int color, Component text, Runnable function) {
            super(id, icon, color, text);
            this.function = function;
        }

        public MenuAction(ResourceLocation id, ResourceLocation icon, Component text, Runnable function) {
            this(id, icon, -1, text, function);
        }

        public MenuAction(ResourceLocation id, Component text, Runnable function) {
            this(id, null, -1, text, function);
        }
    }

    public static class MenuFolder extends MenuItem {
        private final LinkedList<MenuItem> children = new LinkedList<>();

        public MenuFolder(ResourceLocation id, ResourceLocation icon, int color, Component text, MenuItem ... children) {
            super(id, icon, color, text);
            for(MenuItem item : children) {
                this.add(item);
            }
        }

        public MenuFolder(ResourceLocation id, ResourceLocation icon, Component text, MenuItem ... children) {
            this(id, icon, -1, text, children);
        }

        public MenuFolder(ResourceLocation id, Component text, MenuItem ... children) {
            this(id, null, -1, text, children);
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
