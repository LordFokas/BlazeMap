package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.IntHolder;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class WorldMapPopup implements Widget {
    private static final TranslatableComponent MENU_ROOT_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.root");
    private static final TranslatableComponent MENU_BLOCK_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.block");
    private static final TranslatableComponent MENU_CHUNK_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.chunk");
    private static final TranslatableComponent MENU_REGION_TEXT = Helpers.translate("blazemap.gui.worldmap.menu.region");
    private static final int MIN_WIDTH = 80;

    private final int sizeX, sizeY, middleY;
    private int posX, posY, anchorX, anchorY;
    private final boolean invertX, invertY;
    private final ArrayList<PopupItem> items = new ArrayList<>();
    private WorldMapPopup activeChild = null;

    public WorldMapPopup(Coordination coordination, int width, int height, List<BlazeRegistry.Key<Layer>> layers) {
        // Kick off menu creation
        MapMenuSetupEvent.MenuFolder block = new MapMenuSetupEvent.MenuFolder(BlazeMapReferences.MapMenu.MENU_BLOCK, null, MENU_BLOCK_TEXT);
        MapMenuSetupEvent.MenuFolder chunk = new MapMenuSetupEvent.MenuFolder(BlazeMapReferences.MapMenu.MENU_CHUNK, null, MENU_CHUNK_TEXT);
        MapMenuSetupEvent.MenuFolder region = new MapMenuSetupEvent.MenuFolder(BlazeMapReferences.MapMenu.MENU_REGION, null, MENU_REGION_TEXT);
        MapMenuSetupEvent.MenuFolder container = new MapMenuSetupEvent.MenuFolder(BlazeMapReferences.MapMenu.MENU_ROOT, null, MENU_ROOT_TEXT, block, chunk, region);
        MinecraftForge.EVENT_BUS.post(new MapMenuSetupEvent(container, layers, coordination.blockX, coordination.blockZ, coordination.chunkX, coordination.chunkZ, coordination.regionX, coordination.regionZ));

        middleY = height / 2;

        // Materialize self and children; determine width
        sizeY = this.materialize(container);
        int widest = MIN_WIDTH;
        for(PopupItem item : items){
            if(item.width > widest){
                widest = item.width;
            }
        }
        sizeX = widest;

        // Set up position and dimensions
        anchorX = coordination.mousePixelX;
        anchorY = coordination.mousePixelY;
        invertX = anchorX > width / 2;
        invertY = anchorY > middleY;
        spawnFrom(null, 0);

        setMouse(coordination.mousePixelX, coordination.mousePixelY);
    }

    private WorldMapPopup(int anchorX, int anchorY, MapMenuSetupEvent.MenuFolder container, boolean invertX, int middleY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.invertX = invertX;
        this.invertY = anchorY > middleY;
        this.middleY = middleY;

        sizeY = this.materialize(container);
        int widest = MIN_WIDTH;
        for(PopupItem item : items){
            if(item.width > widest){
                widest = item.width;
            }
        }
        sizeX = widest;
    }

    private int materialize(MapMenuSetupEvent.MenuFolder container) {
        final IntHolder offset = new IntHolder();
        container.consume(item -> {
            boolean isFolder = false, isEnabled = true;
            Consumer<WorldMapPopup> function = null;
            if(item instanceof MapMenuSetupEvent.MenuAction action) {
                function = parent -> action.function.run();
            } else if (item instanceof MapMenuSetupEvent.MenuFolder folder) {
                isFolder = true;
                isEnabled = folder.size() > 0;
                WorldMapPopup child = new WorldMapPopup(0, 0, folder, invertX, middleY);
                final int folderY = offset.get();
                function = parent -> {
                    clearChild();
                    activeChild = child;
                    child.spawnFrom(parent, folderY);
                };
            }
            items.add(new PopupItem(item, isFolder, isEnabled, function));
            offset.add(PopupItem.HEIGHT);
        });
        return offset.get();
    }

    private void spawnFrom(WorldMapPopup parent, int folderY) {
        if(parent != null) {
            anchorX = parent.anchorX + parent.sizeX * (invertX ? -1 : 1);
            anchorY = parent.posY + folderY + (invertY ? PopupItem.HEIGHT : 0);
        }

        posX = anchorX - (invertX ? sizeX : 0);
        posY = anchorY - (invertY ? sizeY : 0);
    }

    private boolean intercepts(double mouseX, double mouseY) {
        return interceptsSelf(mouseX, mouseY) || ( activeChild != null && activeChild.intercepts(mouseX, mouseY));
    }

    private boolean interceptsSelf(double mouseX, double mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + sizeX && mouseY <= posY + sizeY;
    }

    public void setMouse(int mouseX, int mouseY) {
        if(activeChild != null) {
            activeChild.setMouse(mouseX, mouseY);
        }
        // TODO: do the thing
    }

    private void clearChild() {
        if(activeChild != null) {
            activeChild.clearChild();
            activeChild = null;
        }
    }

    public ActionResult onClick(int mouseX, int mouseY, int button) {
        if(!intercepts(mouseX, mouseY)) {
            return ActionResult.MISSED;
        } else {
            return onClickInternal(mouseX, mouseY, button);
        }
    }

    private ActionResult onClickInternal(int mouseX, int mouseY, int button) {
        if(interceptsSelf(mouseX, mouseY)) {
            return handleClick(mouseX, mouseY, button);
        } else {
            return activeChild.onClickInternal(mouseX, mouseY, button);
        }
    }

    private ActionResult handleClick(int mouseX, int mouseY, int button) {
        return ActionResult.HANDLED; // TODO: implement
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        stack.pushPose();

        stack.translate(anchorX, anchorY, 1);
        stack.scale(2, 2, 1);
        stack.translate(posX - anchorX, posY - anchorY, 0);

        RenderHelper.fillRect(stack.last().pose(), sizeX, sizeY, Colors.WIDGET_BACKGROUND);
        for(PopupItem item : items) {
            stack.pushPose();
            item.render(stack, sizeX);
            stack.popPose();
            stack.translate(0, PopupItem.HEIGHT, 0);
        }

        stack.popPose();

        if(activeChild != null) {
            activeChild.render(stack, i0, i1, f0);
        }
    }

    public enum ActionResult {
        MISSED(false, true),
        HANDLED(true, false),
        FINISHED(true, true);

        public final boolean wasHandled, shouldDismiss;

        ActionResult(boolean handled, boolean dismiss) {
            this.wasHandled = handled;
            this.shouldDismiss = dismiss;
        }
    }

    private static class PopupItem {
        private static final Font font = Minecraft.getInstance().font;
        public static final int HEIGHT = 12;

        public final int width;
        public final boolean folder, enabled;

        private final Component text;
        private final ResourceLocation icon;
        private final Consumer<WorldMapPopup> function;

        PopupItem(MapMenuSetupEvent.MenuItem item, boolean folder, boolean enabled, Consumer<WorldMapPopup> function) {
            this.text = item.text;
            this.icon = item.icon;
            this.width = 20 + font.width(item.text);
            this.folder = folder;
            this.enabled = enabled;
            this.function = function;
        }

        public void render(PoseStack stack, int width) {
            int color = enabled ? Colors.WHITE : Colors.DISABLED;
            if(icon != null) RenderHelper.drawTexturedQuad(icon, color, stack, 1,2, 8, 8);
            font.draw(stack, text, 10, 3, color);
            if(folder) font.draw(stack, ">", width-8, 3, color);
        }

        public void clicked(WorldMapPopup parent) {
            function.accept(parent);
        }
    }
}
