package com.eerussianguy.blazemap.feature.waypoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointChannelLocal;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointPool;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.IconTabs;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.TreeContainer;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointManagerFragment extends BaseFragment {

    public WaypointManagerFragment() {
        super(Helpers.translate("blazemap.gui.waypoint_manager.title"), true, false);
    }

    @Override
    public void compose(FragmentContainer container) {
        WaypointServiceClient client = WaypointServiceClient.instance();
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }

        IconTabs tabs = new IconTabs().setSize(160, 20).setLine(5, 5);
        container.add(tabs, 0, y);

        for(var pool : client.getPools()) {
            var pc = new PoolContainer(container::dismiss, pool);
            container.add(pc, 0, y + 23);
            tabs.add(pc);
        }
    }

    // =================================================================================================================
    private static class PoolContainer extends FragmentContainer implements IconTabs.TabComponent {
        private final List<Component> tooltip = new ArrayList<>();
        private final WaypointPool pool;

        private PoolContainer(Runnable dismiss, WaypointPool pool) {
            super(dismiss, 0);
            this.pool = pool;
            tooltip.add(pool.getName());
            tooltip.add(new TextComponent("Blaze Map").withStyle(ChatFormatting.BLUE)); // TODO: temporary
            construct();
        }

        private void construct() {
            clear();
            var list = new TreeContainer().setSize(160, 160);
            add(list, 0, 0);
            var groups = pool.getGroups(Helpers.levelOrThrow().dimension());
            for(var group : groups) {
                list.addItem(new NodeItem(group));
            }
            var addButton = new TextButton(new TextComponent("Add Group"), button -> {
                pool.getGroups(Helpers.levelOrThrow().dimension()).add(WaypointGroup.make(WaypointChannelLocal.GROUP_DEFAULT));
                PoolContainer.this.construct();
            }).setSize(79, 14);
            addButton.setEnabled(pool.type.canCreate);
            add(addButton, 0, 162);
        }

        @Override
        public ResourceLocation getIcon() {
            return pool.icon;
        }

        @Override
        public int getIconTint() {
            return pool.tint;
        }

        @Override
        public Component getName() {
            return pool.getName();
        }

        @Override
        public List<Component> getTooltip() {
            return tooltip;
        }
    }

    // =================================================================================================================
    private static class NodeItem extends TreeNode implements TreeContainer.TreeItem, BorderedComponent, ComponentSounds, GuiEventListener {
        private final WaypointGroup group;
        private final List<? extends TreeContainer.TreeItem> children;
        private Runnable updater;
        private boolean open = true;

        private NodeItem(WaypointGroup group) {
            super(group.getName());
            this.group = group;
            this.children = group.getAll().stream().map(LeafItem::new).toList();
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            renderFlatBackground(stack, 0xFF444444);
            stack.translate(12, getHeight() / 2F - 4, 0);
            font.draw(stack, open ? "v" : ">", -9, 1, Colors.BLACK);
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override @SuppressWarnings("unchecked")
        public List<TreeContainer.TreeItem> getChildren() {
            if(open) {
                return (List<TreeContainer.TreeItem>) children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }

        @Override
        public void setUpdater(Runnable function) {
            this.updater = function;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.open = !this.open;
            updater.run();

            if(open) playUpSound();
            else playDownSound();

            return true;
        }
    }

    // =================================================================================================================
    private static class LeafItem extends TreeNode implements TreeContainer.TreeItem, BorderedComponent {
        private static final int ICON_SIZE = 8;
        private final Waypoint waypoint;

        private LeafItem(Waypoint waypoint) {
            super(waypoint.getName());
            this.waypoint = waypoint;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            int offset = (getHeight() - ICON_SIZE) / 2;
            if(hasMouse && mouseIntercepts(mouseX, mouseY)) {
                renderFlatBackground(stack, 0xFF222222); // render hover
            }
            RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), stack, offset, offset, ICON_SIZE, ICON_SIZE);
            stack.translate(getHeight(), getHeight() / 2F - 4, 0);
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }
    }

    // =================================================================================================================
    private static class TreeNode extends Label implements TreeContainer.TreeItem, BorderedComponent {
        private TreeNode(Component text) {
            super(text);
        }

        private TreeNode(String text) {
            super(text);
        }

        @Override
        public int getWidth() {
            return getParent().getWidth();
        }

        @Override
        public int getTextHeight() {
            return 12;
        }
    }
}
