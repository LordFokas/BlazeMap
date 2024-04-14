package com.eerussianguy.blazemap.config;

import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec.*;

import com.eerussianguy.blazemap.BlazeMap;

public class CommonConfig {
    public final BooleanValue enableServerEngine;

    CommonConfig(Builder innerBuilder) {
        Function<String, Builder> builder = name -> innerBuilder.translation(BlazeMap.MOD_ID + ".config.common." + name);

        innerBuilder.push("general");
        enableServerEngine = builder.apply("enableServerEngine")
            .comment(
                "Enable Server Engine on the Integrated Server",
                "This is disabled by default because it is currently buggy and adds no valuable features at the time"
            )
            .define("enableServerEngine", false);
        innerBuilder.pop();
    }
}
