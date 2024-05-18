package com.eerussianguy.blazemap.config;

import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;

import com.eerussianguy.blazemap.BlazeMap;

public class CommonConfig {
    public final FeaturesConfig clientFeatures;
    public final BooleanValue enableServerEngine;

    CommonConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(BlazeMap.MOD_ID + ".config.common." + name);

        innerBuilder.push("general");
        enableServerEngine = builder.apply("enableServerEngine")
            .comment("Enable the Server side Blaze Map Engine (integrated and dedicated)")
            .define("enableServerEngine", true);
        innerBuilder.pop();

        innerBuilder.comment("Adjust what features are available to the player.");
        innerBuilder.comment("Comming Eventually: Features disallowed by server override client's config");
        innerBuilder.push("clientFeatures");
        clientFeatures = new FeaturesConfig(builder);
        innerBuilder.pop();

    }

    public static class FeaturesConfig {
        public final BooleanValue displayCoords;
        public final BooleanValue displayFriendlyMobs;
        public final BooleanValue displayHostileMobs;
        public final BooleanValue displayOtherPlayers;
        public final BooleanValue displayWaypointsOnMap;
        public final BooleanValue renderWaypointsInWorld;

        FeaturesConfig(Function<String, Builder> builder) {
            this.displayCoords = builder.apply("displayCoords")
                .comment("Enables current coordinates to render under minimap")
                .define("displayCoords", true);

            this.displayFriendlyMobs = builder.apply("displayFriendlyMobs")
                .comment("Enables markers showing the location of nearby friendly mobs")
                .define("displayFriendlyMobs", true);
            this.displayHostileMobs = builder.apply("displayHostileMobs")
                .comment("Enables markers showing the location of nearby hostile mobs")
                .define("displayHostileMobs", true);
            this.displayOtherPlayers = builder.apply("displayOtherPlayers")
                .comment("Enables markers showing the location of other players")
                .define("displayOtherPlayers", true);

            this.displayWaypointsOnMap = builder.apply("displayWaypointsOnMap")
                .comment("Enables waypoints to be shown on the map itself")
                .define("displayWaypointsOnMap", true);
            this.renderWaypointsInWorld = builder.apply("renderWaypointsInWorld")
                .comment("Enables waypoints to be rendered in the world")
                .define("renderWaypointsInWorld", false);
        }
    }
}
