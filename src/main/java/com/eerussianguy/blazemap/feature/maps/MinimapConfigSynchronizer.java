package com.eerussianguy.blazemap.feature.maps;

import com.eerussianguy.blazemap.config.ClientConfig.MinimapConfig;

public class MinimapConfigSynchronizer extends MapConfigSynchronizer {

    public MinimapConfigSynchronizer(MapRenderer map, MinimapConfig config) {
        super(map, config);
    }

    private MinimapConfig config(){
        return (MinimapConfig) this.config;
    }

    public void resize(int x, int y){
        MinimapConfig config = config();
        if(x != 0){
            config.width.set(config.width.get() + x);
        }
        if(y != 0){
            config.height.set(config.height.get() + y);
        }
        load();
    }

    public void move(int x, int y){
        MinimapConfig config = config();
        if(x != 0){
            config.positionX.set(config.positionX.get() + x);
        }
        if(y != 0){
            config.positionY.set(config.positionY.get() + y);
        }
        load();
    }

    @Override
    public void load() {
        super.load();
        renderer.resize(config().width.get(), config().height.get());
    }
}