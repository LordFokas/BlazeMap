package com.eerussianguy.blazemap.lib.gui.fragment;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.eerussianguy.blazemap.lib.gui.core.AbsoluteContainer;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.MetaContainer;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.mojang.blaze3d.vertex.PoseStack;

public class HostScreen extends Screen implements TooltipService {
    private final BaseFragment fragment;

    public HostScreen(BaseFragment fragment) {
        super(fragment.getTitle());
        this.fragment = fragment;
    }

    @Override
    protected void init() {
        MetaContainer root = new MetaContainer(width, height);
        AbsoluteContainer main = new AbsoluteContainer(0);
        FragmentContainer container = new FragmentContainer(this::onClose, 5).withBackground();
        AbsoluteContainer extra = new AbsoluteContainer(0);
        root.add(main, extra);

        fragment.compose(container, extra);
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
}
