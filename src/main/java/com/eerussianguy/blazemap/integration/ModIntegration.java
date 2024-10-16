package com.eerussianguy.blazemap.integration;

public abstract class ModIntegration {
    public final String modID;
    final String[] dependencies;

    public ModIntegration(String modID, String ... dependencies) {
        this.modID = modID;
        this.dependencies = dependencies;
    }

    public boolean enabled() {
        return KnownMods.isLoaded(modID);
    }

    public final void init() {
        if(enabled()) {
            KnownMods.addIntegration(this);
            setup();
        }
    }

    public abstract void setup();
}