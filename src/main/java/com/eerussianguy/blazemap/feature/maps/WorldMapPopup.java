package com.eerussianguy.blazemap.feature.maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;

import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class WorldMapPopup implements Widget, GuiEventListener {
    private final int posX, posY, sizeX, sizeY, anchorX, anchorY;

    public WorldMapPopup(Coordination coordination, int width, int height) {
        int posX = anchorX = coordination.blockPixelX;
        int posY = anchorY = coordination.blockPixelY;
        sizeX = 120;
        sizeY = 200;
        if(posX > width / 2) {
            posX -= sizeX;
        }
        if(posY > height / 2) {
            posY -= sizeY;
        }
        this.posX = posX;
        this.posY = posY;
    }

    public boolean intercepts(double mouseX, double mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + sizeX && mouseY <= posY + sizeY;
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        stack.translate(anchorX, anchorY, 1);
        stack.scale(2, 2, 1);
        stack.translate(posX - anchorX, posY - anchorY, 0);

        Font font = Minecraft.getInstance().font;
        RenderHelper.fillRect(stack.last().pose(), sizeX, sizeY, Colors.WIDGET_BACKGROUND);
        font.draw(stack, "Zhu Li Moon", 2, 2, 0x008000);
        font.draw(stack, "Do the thing!", 2, 12, Colors.WHITE);
    }
}
