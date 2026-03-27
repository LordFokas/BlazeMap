package com.eerussianguy.blazemap.profiling.overlay;

import com.eerussianguy.blazemap.integration.KnownMods;

public class VersionString extends StringSource {
    public VersionString(String string) {
        super(string);
    }

    public VersionString(String string, int color) {
        super(string, color);
    }

    @Override
    public StringSource note(String note, int color) {
        return note(() -> note, note.equals(KnownMods.DEVELOPMENT_VERSION) ? 0x00FFFF : color);
    }
}
