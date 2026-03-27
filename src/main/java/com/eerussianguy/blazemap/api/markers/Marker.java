package com.eerussianguy.blazemap.api.markers;

import java.awt.Color;
import java.util.Collections;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;

public class Marker<T extends Marker<T>> {
    private final ResourceLocation id;
    private ResourceKey<Level> dimension;
    private BlockPos.MutableBlockPos position;
    private ResourceLocation icon;
    private int width = 32, height = 32;
    private String name = null;
    private boolean nameVisible = false;
    private int color = -1;
    private float rotation = 0F;
    private boolean usesZoom = false;
    private final Set<String> tags;
    private BlazeRegistry.Key<ObjectRenderer<?>> renderer = BlazeMapReferences.ObjectRenderers.DEFAULT;

    @SuppressWarnings("unchecked")
    public Marker(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, ResourceLocation icon) {
        this(id, dimension, position, icon, (Set<String>) Collections.EMPTY_SET);
    }

    public Marker(ResourceLocation id, ResourceKey<Level> dimension, BlockPos position, ResourceLocation icon, Set<String> tags) {
        this.id = id;
        this.dimension = dimension;
        this.position = new BlockPos.MutableBlockPos().set(position);
        this.icon = icon;
        this.tags = tags;
    }

    public final ResourceLocation getID() {
        return id;
    }

    public final ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPosition() {
        return position;
    }

    /**
     * Used to set a MutableBlockPos object you control.
     * You most likely won't need this.
     * Maybe see setPosition() instead?
     */
    @SuppressWarnings("unchecked")
    public T setPositionObject(BlockPos.MutableBlockPos position) {
        this.position = position;
        return (T) this;
    }

    public BlazeRegistry.Key<ObjectRenderer<?>> getRenderer() {
        return renderer;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    public boolean isNameVisible() {
        return nameVisible;
    }

    public int getColor() {
        return color;
    }

    public float getRotation() {
        return rotation;
    }

    public boolean getUsesZoom() {
        return usesZoom;
    }

    public final Set<String> getTags() {
        return tags;
    }

    @SuppressWarnings("unchecked")
    public T setDimension(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setPosition(BlockPos position) {
        this.position.set(position);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setRenderer(BlazeRegistry.Key<ObjectRenderer<?>> renderer) {
        this.renderer = renderer;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setIcon(ResourceLocation icon) {
        this.icon = icon;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return (T) this;
    }

    public T setSize(int size) {
        return setSize(size, size);
    }

    @SuppressWarnings("unchecked")
    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setNameVisible(boolean visible) {
        this.nameVisible = visible;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setColor(int color) {
        this.color = color;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setRotation(float rotation) {
        this.rotation = rotation;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUsesZoom(boolean usesZoom) {
        this.usesZoom = usesZoom;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T randomizeColor() {
        float hue = ((float) System.nanoTime() % 360) / 360F;
        this.color = Color.HSBtoRGB(hue, 1, 1);
        return (T) this;
    }
}
