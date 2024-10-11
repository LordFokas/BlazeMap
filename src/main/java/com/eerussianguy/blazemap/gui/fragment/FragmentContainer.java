package com.eerussianguy.blazemap.gui.fragment;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.gui.lib.BaseComponent;
import com.eerussianguy.blazemap.gui.lib.BaseContainer;
import com.eerussianguy.blazemap.util.Colors;
import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.vertex.PoseStack;

public class FragmentContainer extends BaseContainer<FragmentContainer> {
    public final Optional<Consumer<Component>> titleConsumer;
    private Runnable dismiss;
    private final int padding;
    private int background;

    public FragmentContainer(Runnable dismiss, int padding) {
        this(dismiss, Optional.empty(), padding);
    }

    public FragmentContainer(Runnable dismiss, Consumer<Component> consumer, int padding) {
        this(dismiss, Optional.of(consumer), padding);
    }

    protected FragmentContainer(Runnable dismiss, Optional<Consumer<Component>> titleConsumer, int padding) {
        this.titleConsumer = titleConsumer;
        this.padding = padding;
        this.dismiss = dismiss;
    }

    public void dismiss() {
        dismiss.run();
    }

    public void add(BaseComponent<?> child, int positionX, int positionY) {
        super.add(child.setPosition(positionX + padding, positionY + padding));
    }

    @Override
    protected void renderBackground(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
        RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), background);
    }

    @Override
    public int getWidth() {
        return max(c -> c.getPositionX() + c.getWidth()) + padding;
    }

    @Override
    public int getHeight() {
        return max(c -> c.getPositionY() + c.getHeight()) + padding;
    }

    public FragmentContainer withBackground(int color) {
        this.background = color;
        return this;
    }

    public FragmentContainer withBackground() {
        return withBackground(Colors.WIDGET_BACKGROUND);
    }
}
