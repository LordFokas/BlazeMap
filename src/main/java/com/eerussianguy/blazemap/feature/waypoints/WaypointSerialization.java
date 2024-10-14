package com.eerussianguy.blazemap.feature.waypoints;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.markers.Waypoint;
import com.eerussianguy.blazemap.api.util.MinecraftStreams;
import com.eerussianguy.blazemap.lib.io.FormatSpec;
import com.eerussianguy.blazemap.lib.io.Format;
import com.eerussianguy.blazemap.lib.io.FormatVersion;

public class WaypointSerialization {
    public static final Format<Map<ResourceLocation, Waypoint>> FORMAT = new FormatSpec<>((byte) 0x00, Formats.legacy)
        .setLegacyLoader(Formats.legacy::read).freeze();

    public static class Formats {
        public static final FormatVersion<Map<ResourceLocation, Waypoint>> legacy = new FormatVersion<>() {
            @Override
            public void write(Map<ResourceLocation, Waypoint> data, MinecraftStreams.Output output) throws IOException {
                output.writeInt(data.size());
                for(Waypoint waypoint : data.values()) {
                    output.writeResourceLocation(waypoint.getID());
                    output.writeDimensionKey(waypoint.getDimension());
                    output.writeBlockPos(waypoint.getPosition());
                    output.writeUTF(waypoint.getName());
                    output.writeResourceLocation(waypoint.getIcon());
                    output.writeInt(waypoint.getColor());
                    output.writeFloat(waypoint.getRotation());
                }
            }

            @Override
            public Map<ResourceLocation, Waypoint> read(MinecraftStreams.Input input) throws IOException {
                Map<ResourceLocation, Waypoint> data = new HashMap<>();
                int count = input.readInt();
                for(int i = 0; i < count; i++) {
                    Waypoint waypoint = new Waypoint(
                        input.readResourceLocation(),
                        input.readDimensionKey(),
                        input.readBlockPos(),
                        input.readUTF(),
                        input.readResourceLocation(),
                        input.readInt()
                    ).setRotation(input.readFloat());
                    data.put(waypoint.getID(), waypoint);
                }
                return data;
            }
        };
    }
}
