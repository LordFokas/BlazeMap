package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.ClientConfig.MinimapConfig;

public class MinimapConfigSynchronizer extends MapConfigSynchronizer {
    private final MinimapConfig config;

    public MinimapConfigSynchronizer(MapRenderer map, MinimapConfig config) {
        super(map, config);
        this.config = config;
    }

    public void setSize(int width, int height){
        config.width.set(width);
        config.height.set(height);
        this.onWidgetChanged();
    }

    public void setPosition(int x, int y){
        config.positionX.set(x);
        config.positionY.set(y);
        this.onWidgetChanged();
    }

    private void onWidgetChanged(){

    }
}