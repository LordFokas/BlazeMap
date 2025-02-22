package com.eerussianguy.blazemap.lib.gui.components.selection;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.core.BaseComponent;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.trait.FocusableComponent;
import com.mojang.blaze3d.vertex.PoseStack;

public class DropdownList<T> extends BaseComponent<DropdownList<T>> implements FocusableComponent {
    protected static final ResourceLocation ARROW_DOWN = BlazeMap.resource("textures/gui/arrow_down.png");
    protected static final ResourceLocation ARROW_UP = BlazeMap.resource("textures/gui/arrow_up.png");

    protected final EdgeReference arrow;
    protected final VolatileContainer volatiles;
    protected final Materializer<T> materializer;
    protected final SelectionList<?, ?> list;
    protected final SelectionModelSingle<T> model;
    private BaseComponent<?> selected = null;
    private boolean open = false;

    public DropdownList(VolatileContainer volatiles, Materializer<T> materializer) {
        this.volatiles = volatiles;
        this.materializer = materializer;
        this.model = new SelectionModelSingle<>();
        this.list = new SelectionList<>(materializer, model).adaptive();

        this.model.addSelectionListener(item -> {
            if(item == null) {
                selected = null;
            }
            else {
                selected = materializer.transform(item);
                selected.setPosition(2, 2);
            }
        });

        this.arrow = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setPosition(3, 3);
    }

    public SelectionModelSingle<T> getModel() {
        return model;
    }

    @Override
    public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        renderFocusableBackground(stack);

        var icon = open ? ARROW_UP : ARROW_DOWN;
        RenderHelper.drawTexturedQuad(icon, getFocusColor(), stack, arrow.getPositionX(), arrow.getPositionY(), arrow.getWidth(), arrow.getHeight());

        if(selected != null) {
            if(hasMouse) {
                mouseX -= selected.getPositionX();
                mouseY -= selected.getPositionY();
                hasMouse = selected.mouseIntercepts(mouseX, mouseY);
            }
            stack.translate(selected.getPositionX(), selected.getGlobalPositionY(), 0);
            selected.render(stack, hasMouse, mouseX, mouseY);
        }
    }

    @Override
    public DropdownList<T> setSize(int w, int h) {
        list.setSize(w, h * 5);
        arrow.setSize(h - 6, h - 6);
        return super.setSize(w, h);
    }

    protected void spawnDropdown() {
        volatiles.add(list, getGlobalPositionX(), getGlobalPositionY() + getHeight() - 1);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if(!focused) {
            open = false;
            updateDropdown();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        open = !open;
        updateDropdown();
        return true;
    }

    private void updateDropdown() {
        list.setFocused(open);
        if(open) {
            spawnDropdown();
        } else {
            volatiles.markForRemoval(list);
        }
    }
}
