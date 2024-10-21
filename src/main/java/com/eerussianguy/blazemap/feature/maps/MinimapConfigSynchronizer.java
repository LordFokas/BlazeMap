package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.config.ClientConfig.MinimapConfig;
import com.eerussianguy.blazemap.engine.render.MapRenderer;

public class MinimapConfigSynchronizer extends MapConfigSynchronizer {
    public MinimapConfigSynchronizer(MapRenderer map, MinimapConfig config) {
        super(map, config);
    }

    @Override
    public void load() {
        super.load();
        MinimapConfig config = (MinimapConfig) this.config;
        renderer.resize(config.width.get(), config.height.get());
    }
}