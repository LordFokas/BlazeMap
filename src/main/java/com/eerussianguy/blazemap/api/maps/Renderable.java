package com.eerussianguy.blazemap.api.maps;

import com.mojang.blaze3d.vertex.PoseStack;

public interface Renderable {
    void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY);
    int getWidth();
    int getHeight();
}