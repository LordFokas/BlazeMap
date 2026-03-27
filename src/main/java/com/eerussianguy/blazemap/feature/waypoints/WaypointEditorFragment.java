package com.eerussianguy.blazemap.feature.waypoints;

import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointGroup;
import com.eerussianguy.blazemap.feature.waypoints.service.WaypointServiceClient;
import com.eerussianguy.blazemap.lib.*;
import com.eerussianguy.blazemap.lib.gui.components.*;
import com.eerussianguy.blazemap.lib.gui.components.selection.DropdownList;
import com.eerussianguy.blazemap.lib.gui.components.selection.SelectionGrid;
import com.eerussianguy.blazemap.lib.gui.core.VolatileContainer;
import com.eerussianguy.blazemap.lib.gui.fragment.BaseFragment;
import com.eerussianguy.blazemap.lib.gui.fragment.FragmentContainer;

public class WaypointEditorFragment extends BaseFragment {
    private final Waypoint waypoint;
    private final boolean creating;
    private WaypointGroup defaultGroup = null;

    public WaypointEditorFragment() {
        this(Minecraft.getInstance().player.blockPosition());
    }

    public WaypointEditorFragment(BlockPos pos) {
        this(pos, null);
    }

    public WaypointEditorFragment(BlockPos pos, WaypointGroup defaultGroup) {
        this(new Waypoint(BlazeMap.resource("waypoint/"+System.nanoTime()), Minecraft.getInstance().level.dimension(), pos, "New Waypoint", BlazeMapReferences.Icons.WAYPOINT, Colors.randomBrightColor()), true);
        this.defaultGroup = defaultGroup;
    }

    public WaypointEditorFragment(Waypoint waypoint) {
        this(waypoint, false);
    }

    private WaypointEditorFragment(Waypoint waypoint, boolean creating) {
        super(Helpers.translate("blazemap.gui.waypoint_editor.title"));
        this.waypoint = waypoint;
        this.creating = creating;
    }

    @Override
    public void compose(FragmentContainer container, VolatileContainer volatiles) {
        int y = 0;

        if(container.titleConsumer.isPresent()) {
            container.titleConsumer.get().accept(getTitle());
        } else {
            container.add(new Label(getTitle()), 0, y);
            y = 15;
        }


        // BASIC INFORMATION ===========================================================================================
        container.add(new SectionLabel("Basic Information").setWidth(160), 0, y);

        ObjHolder<String> name = new ObjHolder<>(waypoint.getName());
        container.add(VanillaComponents.makeTextField(font, 160, 14, name), 0, y+=13);

        var pos = waypoint.getPosition();
        IntHolder posX = new IntHolder(pos.getX()), posY = new IntHolder(pos.getY()), posZ = new IntHolder(pos.getZ());
        container.add(VanillaComponents.makeIntField(font, 58, 14, posX), 0  , y += 17);
        container.add(VanillaComponents.makeIntField(font, 38, 14, posY), 61 , y);
        container.add(VanillaComponents.makeIntField(font, 58, 14, posZ), 102, y);

        DropdownList<ResourceKey<Level>> dimensions = new DropdownList<>(volatiles, d -> new Label(d.location().toString()));
        var dimensionsModel = dimensions.getModel();
        dimensionsModel.setElements(RegistryHelper.getAllDimensions());
        dimensionsModel.setSelected(waypoint.getDimension());
        container.add(dimensions.setSize(160, 14), 0, y += 17);

        DropdownList<WaypointGroup> groups = new DropdownList<>(volatiles, g -> new Label(g.getName()));
        var groupsModel = groups.getModel();
        ArraySet<WaypointGroup> groupSet = new ArraySet<>();
        dimensionsModel.addSelectionListener(dimension -> {
            groupSet.clear();
            WaypointServiceClient.instance().getPools().forEach(pool -> {
                pool.getGroups(dimension).stream().filter(g -> g.management.canCreateChild).forEach(groupSet::add);
            });
            groupsModel.setElements(groupSet);
            if(groupsModel.getSelected() == null) {
                if(defaultGroup != null && groupSet.contains(defaultGroup)) {
                    groupsModel.setSelected(defaultGroup);
                } else {
                    groupsModel.selectFirst();
                }
            }
        });
        container.add(groups.setSize(160, 14), 0, y += 17);

        // APPEARANCE ==================================================================================================
        container.add(new SectionLabel("Appearance").setWidth(160), 0, y += 22);
        IntHolder color = new IntHolder(waypoint.getColor());
        float[] hsb = Colors.RGB2HSB(color.get());

        SelectionGrid<ResourceLocation> icons = new SelectionGrid<>(Function.identity(), 16, 1, BlazeMapReferences.Icons.ALL_WAYPOINTS).setSize(160, 0).setInitialValue(waypoint.getIcon());
        container.add(icons, 0, y += 13);

        ImageDisplay display = new ImageDisplay().setSize(48, 48).setImageSize(32, 32).setColor(color::get);
        icons.setListener(display::setImage);
        container.add(display, 0, y += (icons.getHeight() + 3));

        Slider hue = new HueSlider().setSize(58, 14).setValue(hsb[0]);
        container.add(hue, 51, y);

        SBPicker sbPicker = new SBPicker().setSize(48, 48).setHue(hsb[0]).setValue(hsb[1], hsb[2]);
        container.add(sbPicker, 112, y);

        Label hex = new Label(String.format("#%06X", color.get() & ~Colors.ALPHA)).setColor(Colors.UNFOCUSED);
        container.add(hex, 51, y+= 17);

        TextButton reserved = new TextButton(new TextComponent("Reserved"), $ -> {}).setSize(58, 14);
        container.add(reserved, 51, y+= 17);
        reserved.setEnabled(false);
        reserved.addTooltip(new TextComponent("Reserved for a cool feature later,"), new TextComponent("it does nothing yet."));

        hue.setListener(h -> {
            hsb[0] = h;
            color.set(Colors.HSB2RGB(hsb));
            hex.setText(String.format("#%06X", color.get() & ~Colors.ALPHA));
            sbPicker.setHue(h);
        });

        sbPicker.setListener((s, b) -> {
            hsb[1] = s;
            hsb[2] = b;
            color.set(Colors.HSB2RGB(hsb));
            hex.setText(String.format("#%06X", color.get() & ~Colors.ALPHA));
        });


        // SUBMIT ======================================================================================================
        TextButton submit = new TextButton(Helpers.translate("blazemap.gui.button.save"), button -> {
            waypoint.setName(name.get());
            waypoint.setPosition(new BlockPos(posX.get(), posY.get(), posZ.get()));
            waypoint.setDimension(dimensions.getModel().getSelected());
            waypoint.setIcon(icons.getValue());
            waypoint.setColor(color.get());
            WaypointGroup group = groupsModel.getSelected();
            if(creating) {
                group.add(waypoint);
            }
            container.dismiss();
        });
        container.add(submit.setSize(80, 20), 40, y+20);
    }
}