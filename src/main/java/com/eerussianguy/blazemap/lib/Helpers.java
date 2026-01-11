package com.eerussianguy.blazemap.lib;

import java.util.Calendar;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

public class Helpers {
    public static ClientLevel levelOrThrow() {
        return Objects.requireNonNull(Minecraft.getInstance().level);
    }

    @Nullable
    public static LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    // Fog adjustment factor based on the values used in FogRenderer::setupFog for the generic "in air" rendering case
    private static final float FOG_ADJUSTMENT_FACTOR = 1F - 0.05F;

    /** 
     * Get the current render distance in blocks.
     * 
     * If isFogAdjusted == true, then the distance will be shrunk to compensate for the world fog
     * at the boundary of the render distance, ensuring rendered objects are still visible.
     * 
     * Can pass in an existing mc object to save a lookup.
     */
    public static float getRenderDistance() { return getRenderDistance(Minecraft.getInstance(), false); }
    public static float getRenderDistance(Minecraft mc) { return getRenderDistance(mc, false); }
    public static float getRenderDistance(boolean isFogAdjusted) { return getRenderDistance(Minecraft.getInstance(), isFogAdjusted); }
    public static float getRenderDistance(Minecraft mc, boolean isFogAdjusted) {
        float renderDist = mc.options.getEffectiveRenderDistance() * 16;

        if (isFogAdjusted) {
            renderDist *= Helpers.FOG_ADJUSTMENT_FACTOR;
            renderDist -= 4;
        }
        return renderDist;
    }

    /** 
     * Check if pos is within render distance.
     * 
     * If isFogAdjusted == true, then the distance will be shrunk to compensate for the world fog
     * at the boundary of the render distance, ensuring rendered objects are still visible.
     * 
     * Can pass in an existing mc object to save a lookup.
     */
    public static boolean isInRenderDistance(BlockPos pos) { return isInRenderDistance(Minecraft.getInstance(), pos, false); }
    public static boolean isInRenderDistance(Minecraft mc, BlockPos pos) { return isInRenderDistance(mc, pos, false); }
    public static boolean isInRenderDistance(BlockPos pos, boolean isFogAdjusted) { return isInRenderDistance(Minecraft.getInstance(), pos, false); }
    public static boolean isInRenderDistance(Minecraft mc, BlockPos pos, boolean isFogAdjusted) {
        Entity entity = mc.cameraEntity;
        double renderDist = getRenderDistance(mc, isFogAdjusted);
        return entity != null && entity.blockPosition().distSqr(pos) < renderDist * renderDist;
    }

    public static boolean isInFogDistance(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.cameraEntity;
        double fogDist = RenderSystem.getShaderFogStart();
        return entity != null && entity.blockPosition().distSqr(pos) < fogDist * fogDist;
    }

    public static String getServerID() {
        Minecraft mc = Minecraft.getInstance();
        if(mc.hasSingleplayerServer()) {
            return mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        else {
            return mc.getCurrentServer().ip;
        }
    }

    public static boolean isIntegratedServerRunning() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    public static void runOnMainThread(Runnable r) {
        Minecraft.getInstance().tell(r);
    }

    public static TranslatableComponent translate(String key) {
        return new TranslatableComponent(key);
    }

    public static TranslatableComponent translate(String key, Object ... args) {
        return new TranslatableComponent(key, args);
    }

    public static int clamp(int min, int var, int max) {
        return Math.max(min, Math.min(var, max));
    }

    public static float clamp(float min, float var, float max) {
        return Math.max(min, Math.min(var, max));
    }

    public static double clamp(double min, double var, double max) {
        return Math.max(min, Math.min(var, max));
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if(closeable != null) {
            try {closeable.close();}
            catch(Exception ignored) {}
        }
    }

    public static String getISO8601(char d, char t, char h) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // fuck you too Java
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return String.format("%04d%s%02d%s%02d%s%02d%s%02d%s%02d", year, d, month, d, day, t, hour, h, minute, h, second);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T cycle(T current, int direction) {
        T[] values = (T[]) current.getClass().getEnumConstants();
        int index = (current.ordinal() + values.length + direction) % values.length;
        return values[index];
    }
}
