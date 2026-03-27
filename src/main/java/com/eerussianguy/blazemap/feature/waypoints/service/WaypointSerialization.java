package com.eerussianguy.blazemap.feature.waypoints.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.lib.InheritedBoolean;
import com.eerussianguy.blazemap.lib.RegistryHelper;
import com.eerussianguy.blazemap.lib.io.FormatSpec;
import com.eerussianguy.blazemap.lib.io.Format;
import com.eerussianguy.blazemap.lib.io.FormatVersion;

public class WaypointSerialization {
    public static final Format<Map<ResourceKey<Level>, List<WaypointGroup>>> FORMAT
        = new FormatSpec<>((byte) 0x00, new CurrentFormat())
        .setLegacyLoader(OutdatedFormats.legacy)
        .freeze();


    private static class CurrentFormat implements FormatVersion<Map<ResourceKey<Level>, List<WaypointGroup>>> {
        @Override
        public void write(Map<ResourceKey<Level>, List<WaypointGroup>> data, MinecraftStreams.Output output) throws IOException {
            // Write non-empty dimensions
            output.writeCollection(data.entrySet().stream().filter(e -> e.getValue().size() > 0).toList(), entry -> {
                output.writeResourceLocation(entry.getKey().location());

                // Write groups in dimension
                output.writeCollection(entry.getValue(), group -> {
                    output.writeResourceLocation(group.type);
                    if(group.isUserNamed()) {
                        output.writeUTF(group.getNameString());
                    }
                    output.writeByte(group.getState().getVisibility().ordinal());

                    // Write waypoints in group
                    output.writeCollection(group.getAll(), waypoint -> {
                        output.writeResourceLocation(waypoint.getID());
                        output.writeBlockPos(waypoint.getPosition());
                        output.writeUTF(waypoint.getName());
                        output.writeResourceLocation(waypoint.getIcon());
                        output.writeInt(waypoint.getColor());
                        output.writeFloat(waypoint.getRotation());
                        output.writeByte(group.getState(waypoint.getID()).getVisibility().ordinal());
                    });
                });
            });
        }

        @Override
        public Map<ResourceKey<Level>, List<WaypointGroup>> read(MinecraftStreams.Input input) throws IOException {
            Map<ResourceKey<Level>, List<WaypointGroup>> data = new HashMap<>();

            // Read non-empty dimensions
            input.readCollection(() -> {
                ResourceKey<Level> dimension = RegistryHelper.getDimension(input.readResourceLocation());
                ArrayList<WaypointGroup> groups = new ArrayList<>();
                data.put(dimension, groups);

                // Read groups in dimension
                input.readCollection(() -> {
                    WaypointGroup group = WaypointGroup.make(input.readResourceLocation());
                    if(group.isUserNamed()) {
                        group.setUserGivenName(input.readUTF());
                    }
                    group.getState().setVisibility(InheritedBoolean.values()[input.readByte()]);
                    groups.add(group);

                    // Read waypoints in group
                    input.readCollection(() -> {
                        ResourceLocation waypointID = input.readResourceLocation();
                        group.add(new Waypoint(
                            waypointID,
                            dimension,
                            input.readBlockPos(),
                            input.readUTF(),
                            input.readResourceLocation(),
                            input.readInt())
                            .setRotation(input.readFloat())
                        );
                        group.getState(waypointID).setVisibility(InheritedBoolean.values()[input.readByte()]);
                    });
                });
            });

            return data;
        }
    }

    private static class OutdatedFormats {
        private static final FormatVersion.Outdated<Map<ResourceKey<Level>, List<WaypointGroup>>> legacy = input -> {
            Map<ResourceKey<Level>, List<WaypointGroup>> data = new HashMap<>();
            input.readCollection(() -> {
                Waypoint waypoint = new Waypoint(
                    input.readResourceLocation(),
                    readDeprecatedLegacyDimension(input),
                    input.readBlockPos(),
                    input.readUTF(),
                    input.readResourceLocation(),
                    input.readInt()
                ).setRotation(input.readFloat());
                data.computeIfAbsent(waypoint.getDimension(), $ -> new ArrayList<>(List.of(new WaypointGroup(WaypointChannelLocal.GROUP_DEFAULT)))).get(0).add(waypoint);
            });
            return data;
        };

        /** @deprecated storing the registry name is stupid and ResourceLocation suffices  */
        @Deprecated(since="1.18.2-0.8.0", forRemoval = true)
        private static ResourceKey<Level> readDeprecatedLegacyDimension(MinecraftStreams.Input input) throws IOException {
            ResourceLocation registry = input.readResourceLocation();
            ResourceLocation location = input.readResourceLocation();
            return ResourceKey.create(ResourceKey.createRegistryKey(registry), location);
        }
    }
}