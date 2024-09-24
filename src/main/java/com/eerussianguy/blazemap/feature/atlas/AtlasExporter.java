package com.eerussianguy.blazemap.feature.atlas;

import com.eerussianguy.blazemap.engine.BlazeMapAsync;

public class AtlasExporter {
    private static AtlasTask task = null;

    public static synchronized void exportAsync(AtlasTask task) {
        if(AtlasExporter.task == null) {
            AtlasExporter.task = task;
            BlazeMapAsync.instance().clientChain.runOnDataThread(task::exportAsync);
        } else {
            AtlasExporter.task.flash();
        }
    }

    public static synchronized AtlasTask getTask() {
        return task;
    }

    static synchronized void resetTask() {
        task = null;
    }
}