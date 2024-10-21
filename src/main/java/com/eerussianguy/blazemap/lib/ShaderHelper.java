package com.eerussianguy.blazemap.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

public class ShaderHelper {
    private static final HashMap<ResourceLocation, ShaderInstance> shaders = new HashMap<>();
    private static final ArrayList<ShaderInstance> textureShaderStack = new ArrayList<>();

    public static ShaderInstance getTextureShader() {
        if(textureShaderStack.size() > 0) {
            return textureShaderStack.get(0);
        }
        return GameRenderer.getPositionTexShader();
    }

    public static void withTextureShader(ResourceLocation shader, Runnable function) {
        textureShaderStack.add(0, getShader(shader));
        try {
            function.run();
        }
        finally {
            textureShaderStack.remove(0);
        }
    }

    public static ShaderInstance getShader(ResourceLocation name) {
        return shaders.computeIfAbsent(name, $ -> {
            try {
                return new ShaderInstance(Minecraft.getInstance().getResourceManager(), name, DefaultVertexFormat.POSITION_TEX);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
