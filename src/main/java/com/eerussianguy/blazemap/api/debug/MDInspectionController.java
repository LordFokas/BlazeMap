package com.eerussianguy.blazemap.api.debug;

import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.api.pipeline.MasterDatum;

public interface MDInspectionController<MD extends MasterDatum> {
    int getNumLines(MD md);
    String getLine(MD md, int line);

    int getNumGrids(MD md);
    String getGridName(MD md, int grid);
    ResourceLocation getIcon(MD md, int grid, int x, int z);
    int getTint(MD md, int grid, int x, int z);
    String getTooltip(MD md, int grid, int x, int z);
}
