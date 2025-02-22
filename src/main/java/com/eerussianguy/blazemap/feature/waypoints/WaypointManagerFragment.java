package com.eerussianguy.blazemap.feature.waypoints;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.feature.waypoints.service.*;
import com.eerussianguy.blazemap.feature.waypoints.ui.WaypointGroupNode;
import com.eerussianguy.blazemap.integration.KnownMods;
import com.eerussianguy.blazemap.lib.*;
import com.eerussianguy.blazemap.lib.gui.components.IconTabs;
import com.eerussianguy.blazemap.lib.gui.components.Label;
import com.eerussianguy.blazemap.lib.gui.components.Tree;
import com.eerussianguy.blazemap.lib.gui.components.TextButton;
import com.eerussianguy.blazemap.lib.gui.components.selection.DropdownList;
import com.eerussianguy.blazemap.lib.gui.components.selection.SelectionModelSingle;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;

public class WaypointManagerFragment extends BaseFragment {
    private static final int MANAGER_UI_WIDTH = 200;

    public WaypointManagerFragment() {
        super(Helpers.translate("blazemap.gui.waypoint_manager.title"), true, false);
    }

    @Override
    public void compose(FragmentContainer container, VolatileContainer volatiles) {
        WaypointServiceClient client = WaypointServiceClient.instance();
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }

        DropdownList<ResourceKey<Level>> dimensions = new DropdownList<>(volatiles, d -> new Label(d.location().toString()));
        var model = dimensions.getModel();
        model.setElements(RegistryHelper.getAllDimensions());
        model.setSelected(Helpers.levelOrThrow().dimension());
        container.add(dimensions.setSize(MANAGER_UI_WIDTH, 14), 0, y);

        IconTabs tabs = new IconTabs().setSize(MANAGER_UI_WIDTH, 20).setLine(5, 5);
        container.add(tabs, 0, y += 17);

        for(var pool : client.getPools()) {
            var pc = new PoolContainer(model, container::dismiss, pool);
            container.add(pc, 0, y + 25);
            tabs.add(pc);
        }
    }

    private static class PoolContainer extends FragmentContainer implements IconTabs.TabComponent {
        private final SelectionModelSingle<ResourceKey<Level>> dimensions;
        private final List<Component> tooltip = new ArrayList<>();
        private final WaypointPool pool;

        private PoolContainer(SelectionModelSingle<ResourceKey<Level>> dimensions, Runnable dismiss, WaypointPool pool) {
            super(dismiss, 0);
            this.dimensions = dimensions;
            this.pool = pool;
            tooltip.add(pool.getName());
            tooltip.add(new TextComponent(KnownMods.getOwnerName(pool.id)).withStyle(ChatFormatting.BLUE));
            construct();
        }

        private void construct() {
            clear();

            Tree tree = new Tree().setSize(MANAGER_UI_WIDTH, 160);
            add(tree, 0, 0);
            dimensions.addSelectionListener(dimension -> {
                tree.clearItems();
                var groups = pool.getGroups(dimension);
                for(var group : groups) {
                    tree.addItem(new WaypointGroupNode(group, () -> groups.remove(group)));
                }
            });

            var addButton = new TextButton(Helpers.translate("blazemap.gui.button.add_group"), button -> {
                pool.getGroups(Helpers.levelOrThrow().dimension()).add(WaypointGroup.make(WaypointChannelLocal.GROUP_DEFAULT));
                PoolContainer.this.construct();
            }).setSize(MANAGER_UI_WIDTH / 2 - 1, 14);
            addButton.setEnabled(pool.canUserCreate());
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
}