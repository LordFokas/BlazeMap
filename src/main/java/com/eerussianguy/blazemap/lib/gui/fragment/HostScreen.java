package com.eerussianguy.blazemap.lib.gui.fragment;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.lib.gui.core.*;
import com.mojang.blaze3d.vertex.PoseStack;

public class HostScreen extends Screen implements TooltipService {
    private final BaseFragment fragment;
    private final Runnable callback;

    public HostScreen(BaseFragment fragment) {
        this(fragment, () -> {});
    }

    public HostScreen(BaseFragment fragment, Runnable callback) {
        super(fragment.getTitle());
        this.fragment = fragment;
        this.callback = callback;
    }

    @Override
    protected void init() {
        MetaContainer root = new MetaContainer(width, height);
        AbsoluteContainer main = new AbsoluteContainer(0);
        FragmentContainer container = new FragmentContainer(this::onClose, 5).withBackground();
        VolatileContainer volatiles = new VolatileContainer(0);
        AbsoluteContainer extra = new AbsoluteContainer(0);
        root.add(main, extra, volatiles);

        fragment.compose(container, volatiles, extra);
        main.add(container, ContainerAnchor.MIDDLE_CENTER);

        this.addRenderableWidget(root);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partial) {
        renderBackground(stack);
        super.render(stack, mouseX, mouseY, partial);
    }

    @Override
    public void drawTooltip(PoseStack stack, int x, int y, List<? extends Component> lines) {
        renderTooltip(stack, lines.stream().map(Component::getVisualOrderText).toList(), x, y);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.callback.run();
    }
}
