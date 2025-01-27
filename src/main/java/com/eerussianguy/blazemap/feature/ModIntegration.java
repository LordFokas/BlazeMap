package com.eerussianguy.blazemap.feature;

import java.util.List;

public class ModIntegration {
    public static class ModIDs {
        public static final String MINECRAFT = "minecraft";
        public static final String FORGE = "forge";

        public static final String SODIUM = "sodium";
        public static final String EMBEDDIUM = "embeddium";
        public static final String RUBIDIUM = "rubidium";

        public static final String OPTIFINE = "optifine";
        public static final String FANCY_MENU = "fancymenu";
    }

    public static final List<String> SODIUM_FAMILY = List.of(ModIDs.SODIUM, ModIDs.EMBEDDIUM, ModIDs.RUBIDIUM);
}
