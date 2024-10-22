package com.eerussianguy.blazemap.feature.waypoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.*;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import com.eerussianguy.blazemap.lib.InheritedBoolean;
import com.eerussianguy.blazemap.lib.RenderHelper;
import com.eerussianguy.blazemap.lib.gui.components.IconTabs;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.TreeContainer;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.core.ContainerAnchor;
import com.eerussianguy.blazemap.lib.gui.core.EdgeReference;
import com.eerussianguy.blazemap.lib.gui.core.TooltipService;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;
import com.eerussianguy.blazemap.lib.gui.trait.BorderedComponent;
import com.eerussianguy.blazemap.lib.gui.trait.ComponentSounds;
import com.mojang.blaze3d.vertex.PoseStack;

public class WaypointManagerFragment extends BaseFragment {
    private static final int MANAGER_UI_WIDTH = 200;

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

        IconTabs tabs = new IconTabs().setSize(MANAGER_UI_WIDTH, 20).setLine(5, 5);
        container.add(tabs, 0, y);

        for(var pool : client.getPools()) {
            var pc = new PoolContainer(container::dismiss, pool);
            container.add(pc, 0, y + 25);
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
            var list = new TreeContainer().setSize(MANAGER_UI_WIDTH, 160);
            add(list, 0, 0);
            var groups = pool.getGroups(Helpers.levelOrThrow().dimension());
            for(var group : groups) {
                list.addItem(new NodeItem(group, () -> groups.remove(group)));
            }
            var addButton = new TextButton(new TextComponent("Add Group"), button -> {
                pool.getGroups(Helpers.levelOrThrow().dimension()).add(WaypointGroup.make(WaypointChannelLocal.GROUP_DEFAULT));
                PoolContainer.this.construct();
            }).setSize(MANAGER_UI_WIDTH / 2 - 1, 14);
            addButton.setEnabled(pool.management.canCreate);
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
    private static class NodeItem extends TreeNode {
        private final WaypointGroup group;
        private final ArrayList<? extends TreeContainer.TreeItem> children;
        private boolean open = true;

        private NodeItem(WaypointGroup group, Runnable delete) {
            super(group.getName(), group.getState(), delete);
            this.group = group;
            this.children = new ArrayList<>(group.getAll().stream().map(this::makeChild).toList());
        }

        private LeafItem makeChild(Waypoint waypoint) {
            ResourceLocation id = waypoint.getID();
            return new LeafItem(
                waypoint,
                group.getState(id),
                () -> {
                    group.remove(id);
                }
            );
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            renderFlatBackground(stack, 0xFF444444);
            stack.pushPose();
            stack.translate(getHeight(), getHeight() / 2F - 4, 0);
            font.draw(stack, open ? "v" : ">", -9, 1, Colors.BLACK);
            stack.popPose();
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override @SuppressWarnings("unchecked")
        public List<TreeContainer.TreeItem> getChildren() {
            if(open) {
                children.removeIf(TreeContainer.TreeItem::wasDeleted);
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(super.mouseClicked(mouseX, mouseY, button)) return true;

            this.open = !this.open;
            updater.run();

            if(open) playUpSound();
            else playDownSound();

            return true;
        }

        @Override
        protected boolean isDeletable() {
            return group.management == ManagementType.FULL;
        }
    }

    // =================================================================================================================
    private static class LeafItem extends TreeNode {
        private static final int ICON_SIZE = 8;
        private final Waypoint waypoint;

        private LeafItem(Waypoint waypoint, LocalState state, Runnable delete) {
            super(waypoint.getName(), state, delete);
            this.waypoint = waypoint;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            int offset = (getHeight() - ICON_SIZE) / 2;
            if(hasMouse && mouseIntercepts(mouseX, mouseY)) {
                renderFlatBackground(stack, 0xFF222222); // render hover
            }
            RenderHelper.drawTexturedQuad(waypoint.getIcon(), waypoint.getColor(), stack, offset, offset, ICON_SIZE, ICON_SIZE);
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
    private static class TreeNode extends Label implements TreeContainer.TreeItem, BorderedComponent, ComponentSounds, GuiEventListener {
        private static final ResourceLocation ADD = BlazeMap.resource("textures/gui/add.png");
        private static final ResourceLocation REMOVE = BlazeMap.resource("textures/gui/remove.png");
        private static final ResourceLocation ON_OVERRIDE   = BlazeMap.resource("textures/gui/on.png");
        private static final ResourceLocation OFF_OVERRIDE  = BlazeMap.resource("textures/gui/off.png");
        private static final ResourceLocation ON_INHERITED  = BlazeMap.resource("textures/gui/on_inherited.png");
        private static final ResourceLocation OFF_INHERITED = BlazeMap.resource("textures/gui/off_inherited.png");

        protected final EdgeReference visibility = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(2, 2);
        protected final EdgeReference delete = new EdgeReference(this, ContainerAnchor.TOP_RIGHT).setSize(8, 8).setPosition(12, 2);
        protected final LocalState state;
        protected final Runnable onDelete;
        protected Runnable updater = () -> {};
        private boolean wasDeleted = false;

        private TreeNode(Component text, LocalState state, Runnable delete) {
            super(text);
            this.state = state;
            this.onDelete = delete;
        }

        private TreeNode(String text, LocalState state, Runnable delete) {
            super(text);
            this.state = state;
            this.onDelete = delete;
        }

        @Override
        public void render(PoseStack stack, boolean hasMouse, int mouseX, int mouseY) {
            ResourceLocation texture = switch(state.getVisibility()) {
                case TRUE -> ON_OVERRIDE;
                case FALSE -> OFF_OVERRIDE;
                case INHERITED -> state.isVisible() ? ON_INHERITED : OFF_INHERITED;
            };
            RenderHelper.drawTexturedQuad(texture, Colors.NO_TINT, stack, visibility.getPositionX(), visibility.getPositionY(), visibility.getWidth(), visibility.getHeight());
            if(isDeletable()) {
                RenderHelper.drawTexturedQuad(REMOVE, Screen.hasShiftDown() ? Colors.NO_TINT : Colors.DISABLED, stack, delete.getPositionX(), delete.getPositionY(), delete.getWidth(), delete.getHeight());
            }

            stack.translate(getHeight(), getHeight() / 2F - 4, 0);
            super.render(stack, hasMouse, mouseX, mouseY);
        }

        @Override
        protected void renderTooltip(PoseStack stack, int mouseX, int mouseY, TooltipService service) {
            if(visibility.mouseIntercepts(mouseX, mouseY)) {
                InheritedBoolean visible = state.getVisibility();
                var tooltip = new TextComponent(switch(visible) {
                    case TRUE -> "Visibility: Show";
                    case FALSE -> "Visibility: Hide";
                    case INHERITED -> "Visibility: Inherit";
                });
                service.drawTooltip(stack, mouseX, mouseY, tooltip);
                return;
            }

            if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
                var tooltip = new TextComponent("Delete");
                if(!Screen.hasShiftDown()) {
                    service.drawTooltip(stack, mouseX, mouseY, tooltip, new TextComponent("Hold [Shift] to confirm").withStyle(ChatFormatting.YELLOW));
                } else {
                    service.drawTooltip(stack, mouseX, mouseY, tooltip.withStyle(ChatFormatting.RED));
                }
            }
        }

        protected boolean isDeletable() {
            return true;
        }

        @Override
        public int getColor() {
            return state.isVisible() ? Colors.WHITE : Colors.DISABLED;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(visibility.mouseIntercepts(mouseX, mouseY)) {
                int direction = switch(button) {
                    case GLFW.GLFW_MOUSE_BUTTON_1 -> 1;
                    case GLFW.GLFW_MOUSE_BUTTON_2 -> -1;
                    default -> 0;
                };
                if(direction == 0) {
                    playDeniedSound();
                } else {
                    playOkSound();
                    state.setVisibility(Helpers.cycle(state.getVisibility(), direction));
                }
                return true;
            }
            if(isDeletable() && delete.mouseIntercepts(mouseX, mouseY)) {
                if(Screen.hasShiftDown()) {
                    playOkSound();
                    onDelete.run();
                    wasDeleted = true;
                    updater.run();
                } else {
                    playDeniedSound();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean wasDeleted() {
            return wasDeleted;
        }

        @Override
        public void setUpdater(Runnable function) {
            this.updater = function;
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