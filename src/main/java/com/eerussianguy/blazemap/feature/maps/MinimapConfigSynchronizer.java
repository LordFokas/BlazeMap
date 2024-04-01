package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.ClientConfig.MinimapConfig;

public class MinimapConfigSynchronizer extends MapConfigSynchronizer {

    public MinimapConfigSynchronizer(MapRenderer map, MinimapConfig config) {
        super(map, config);
    }

    private MinimapConfig config(){
        return (MinimapConfig) this.config;
    }

    public void setSize(int width, int height){
        config().width.set(width);
        config().height.set(height);
        this.onWidgetChanged();
    }

    public void setPosition(int x, int y){
        config().positionX.set(x);
        config().positionY.set(y);
        this.onWidgetChanged();
    }

    private void onWidgetChanged(){
        save();
    }

    @Override
    public void load() {
        super.load();
        renderer.resize(config().width.get(), config().height.get());
    }
}