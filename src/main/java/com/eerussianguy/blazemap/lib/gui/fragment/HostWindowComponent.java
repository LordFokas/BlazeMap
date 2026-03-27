package com.eerussianguy.blazemap.lib.gui.fragment;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.LineContainer;
import com.eerussianguy.blazemap.lib.gui.core.*;
import com.eerussianguy.blazemap.lib.gui.util.MouseSubpixelSmoother;
import com.mojang.blaze3d.vertex.PoseStack;

public class HostWindowComponent extends LineContainer {
    private Consumer<? super HostWindowComponent> closer = $ -> {};

    public HostWindowComponent(BaseFragment content, VolatileContainer volatiles) {
        super(ContainerAxis.VERTICAL, ContainerDirection.POSITIVE, 0);

        TitleBar titleBar = new TitleBar(this::close);
        this.add(titleBar);

        FragmentContainer window = new FragmentContainer(this::close, titleBar::setTitle, 5).withBackground();
        content.compose(window, volatiles, null);
        this.add(window);
    }

    private void close() {
        closer.accept(this);
    }

    public HostWindowComponent setCloser(Consumer<? super HostWindowComponent> closer) {
        this.closer = closer;
        return this;
    }

    private static class TitleBar extends BaseComponent<TitleBar> implements GuiEventListener {
        private final MouseSubpixelSmoother mouse = new MouseSubpixelSmoother();
        private final EdgeReference close;
        private final Runnable onClose;
        private Component title = TextComponent.EMPTY;

        public TitleBar(Runnable onClose) {
            this.onClose = onClose;
            this.close = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(10, 10).setPosition(1, 1);
            this.setSize(100, 12);
        }

        public void setTitle(Component title) {
            this.title = title;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            Minecraft mc = Minecraft.getInstance();

            // background
            RenderHelper.fillRect(stack.last().pose(), getWidth(), getHeight(), 0xFF000000);

            // close button
            stack.pushPose();
            stack.translate(close.getPositionX(), close.getPositionY(), 0);
            int red = (hasMouse && close.mouseIntercepts(mouseX, mouseY)) ? 0xFFFF4444 : 0xFFFF0000;
            RenderHelper.fillRect(stack.last().pose(), close.getWidth(), close.getHeight(), red);
            mc.font.draw(stack, "x", 2.5F, 0.5F, Colors.WHITE);
            stack.popPose();

            // title
            mc.font.draw(stack, title, 2, 2, Colors.NO_TINT);
        }

        @Override // stretch to parent width
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override // avoid the stack overflow from the getWidth override
        public int getIndependentWidth() {
            return super.getWidth();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(close.mouseIntercepts(mouseX, mouseY)) {
                onClose.run();
            }
            return true;
        }

        @Override // on drag title bar whole window is dragged
        public boolean mouseDragged(double mouseX, double mouseY, int button, double draggedX, double draggedY) {
            mouse.addMovement(draggedX, draggedY);
            getParent().move(mouse.movementX(), mouse.movementY());
            return true;
        }
    }
}
