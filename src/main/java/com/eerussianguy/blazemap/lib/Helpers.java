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

    public static boolean isInRenderDistance(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.cameraEntity;
        double renderDist = mc.options.getEffectiveRenderDistance() * 16;
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
}
