package com.eerussianguy.blazemap.feature.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;

import com.eerussianguy.blazemap.api.BlazeRegistry;
import com.eerussianguy.blazemap.api.event.MapMenuSetupEvent;
import com.eerussianguy.blazemap.api.maps.Layer;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.Helpers;
import com.eerussianguy.blazemap.util.IntHolder;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class WorldMapPopup implements Widget {
    private static final ResourceLocation MENU_ROOT = Helpers.identifier("map.menu");
    private static final TextComponent MENU_ROOT_TEXT = new TextComponent("");
    private static final int MIN_WIDTH = 160;

    private final int sizeX, sizeY, middleY;
    private int posX, posY, anchorX, anchorY;
    private final boolean invertX, invertY;
    private final ArrayList<PopupItem> items = new ArrayList<>();
    private WorldMapPopup activeChild = null;
    private PopupItem lastClicked, hovered;

    public WorldMapPopup(Coordination coordination, int width, int height, List<BlazeRegistry.Key<Layer>> layers) {
        // Kick off menu creation
        MapMenuSetupEvent.MenuFolder container = new MapMenuSetupEvent.MenuFolder(MENU_ROOT, null, MENU_ROOT_TEXT);
        MinecraftForge.EVENT_BUS.post(new MapMenuSetupEvent(container, layers, coordination.blockX, coordination.blockZ, coordination.chunkX, coordination.chunkZ, coordination.regionX, coordination.regionZ));
        if(container.size() == 0) {
            container.add(WorldMapMenu.NOOP);
        }

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
                isEnabled = action.function != null;
                function = isEnabled ? parent -> action.function.run() : $ -> {};
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
        if(interceptsSelf(mouseX, mouseY)) {
            hovered = getItemAtY(mouseY);
        } else {
            hovered = null;
        }
    }

    private void clearChild() {
        if(activeChild != null) {
            activeChild.clearChild();
            activeChild = null;
        }
        lastClicked = hovered = null;
    }

    public ActionResult onClick(int mouseX, int mouseY, int button) {
        if(!intercepts(mouseX, mouseY)) {
            return ActionResult.MISSED;
        } else {
            ActionResult result = onClickInternal(mouseX, mouseY, button);
            if(result.shouldDismiss) {
                clearChild();
            }
            return result;
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
        PopupItem item = getItemAtY(mouseY);
        item.clicked(this);
        lastClicked = item;
        setMouse(mouseX, mouseY);
        if(item.folder) {
            return ActionResult.HANDLED;
        } else {
            return ActionResult.FINISHED;
        }
    }

    private PopupItem getItemAtY(int y) {
        if(y == posY + items.size() * PopupItem.HEIGHT) {
            return items.get(items.size() - 1);
        }
        return items.get((y - posY) / PopupItem.HEIGHT);
    }

    @Override
    public void render(PoseStack stack, int i0, int i1, float f0) {
        stack.pushPose();

        stack.translate(posX, posY, 1);

        RenderHelper.fillRect(stack.last().pose(), sizeX, sizeY, Colors.WIDGET_BACKGROUND);
        for(PopupItem item : items) {
            stack.pushPose();
            item.render(stack, sizeX, item == lastClicked, item == hovered);
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
        public static final int HEIGHT = 24;

        public final int width;
        public final boolean folder, enabled;

        private final Component text;
        private final ResourceLocation icon;
        private final int iconTint;
        private final Consumer<WorldMapPopup> function;

        PopupItem(MapMenuSetupEvent.MenuItem item, boolean folder, boolean enabled, Consumer<WorldMapPopup> function) {
            this.text = item.text;
            this.icon = item.icon;
            this.iconTint = item.iconColor;
            this.width = 40 + font.width(item.text)*2;
            this.folder = folder;
            this.enabled = enabled;
            this.function = function;
        }

        public void render(PoseStack stack, int width, boolean active, boolean hovered) {
            if(hovered) {
                RenderHelper.fillRect(stack.last().pose(), width, HEIGHT, 0x40808080);
            }

            int color = enabled ? (active ? 0xFFDD00 : Colors.WHITE) : Colors.DISABLED;
            if(icon != null) RenderHelper.drawTexturedQuad(icon, enabled ? iconTint : color, stack, 2,4, 16, 16);

            stack.pushPose();
            stack.scale(2, 2, 1);
            font.draw(stack, text, 10, 3, color);
            if(folder) font.draw(stack, ">", (width/2) - 8, 3, color);
            stack.popPose();
        }

        public void clicked(WorldMapPopup parent) {
            function.accept(parent);
        }
    }
}
