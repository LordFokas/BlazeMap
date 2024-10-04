package com.eerussianguy.blazemap.feature.atlas;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.Direction;

import com.eerussianguy.blazemap.api.util.RegionPos;

class AtlasPage {
    private static final int RANGE = 3;
    private int startX, startZ, endX, endZ;
    private int regionsX, regionsZ, grossArea;
    private RegionPos centerOfMass;
    private final Set<RegionPos> regions = new HashSet<>();

    public AtlasPage(RegionPos first) {
        regions.add(first);
    }

    public AtlasPage(RegionPos common, Collection<AtlasPage> neighbors) {
        for(var neighbor : neighbors) {
            regions.addAll(neighbor.regions);
        }
        regions.add(common);
    }

    public boolean isAdjacent(RegionPos candidate) {
        for(var region : regions) {
            if(Math.abs(region.x - candidate.x) <= RANGE && Math.abs(region.z - candidate.z) <= RANGE) {
                return true;
            }
        }
        return false;
    }

    public void add(RegionPos pos) {
        regions.add(pos);
    }

    public void shrink(RegionPos core) {
        Direction worst = null;
        double score = 0;

        if(startX < core.x) {
            int xNegDist = Math.abs(core.x - startX);
            double xNegFill = ((double) regions.stream().filter(r -> r.x == startX).count()) / (double) regionsZ; // must divide by other axis length
            double xNegScore = xNegDist * xNegDist / xNegFill; // distance squared divided by fill, to punish distance harder
            if(xNegScore > score) {
                score = xNegScore;
                worst = Direction.WEST;
            }
        }

        if(startZ < core.z) {
            int zNegDist = Math.abs(core.z - startZ);
            double zNegFill = ((double) regions.stream().filter(r -> r.z == startZ).count()) / (double) regionsX; // must divide by other axis length
            double zNegScore = zNegDist * zNegDist / zNegFill; // distance squared divided by fill, to punish distance harder
            if(zNegScore > score) {
                score = zNegScore;
                worst = Direction.SOUTH;
            }
        }

        if(endX > core.x) {
            int xPosDist = Math.abs(endX - core.x);
            double xPosFill = ((double) regions.stream().filter(r -> r.x == endX).count()) / (double) regionsZ; // must divide by other axis length
            double xPosScore = xPosDist * xPosDist / xPosFill; // distance squared divided by fill, to punish distance harder
            if(xPosScore > score) {
                score = xPosScore;
                worst = Direction.EAST;
            }
        }

        if(endZ > core.z) {
            int zPosDist = Math.abs(endZ - core.z);
            double zPosFill = ((double) regions.stream().filter(r -> r.z == endZ).count()) / (double) regionsX; // must divide by other axis length
            double zPosScore = zPosDist * zPosDist / zPosFill; // distance squared divided by fill, to punish distance harder
            if(zPosScore > score) {
                score = zPosScore;
                worst = Direction.NORTH;
            }
        }

        var rejected = (switch(worst) {
            case WEST -> regions.stream().filter(r -> r.x == startX);
            case EAST -> regions.stream().filter(r -> r.x == endX);
            case SOUTH -> regions.stream().filter(r -> r.z == startZ);
            case NORTH -> regions.stream().filter(r -> r.z == endZ);
            default -> throw new IllegalStateException("Unexpected value: " + worst);
        }).toList();

        rejected.forEach(regions::remove);

        calculateSize();
    }

    public void calculateSize() {
        int cumulativeX = 0, cumulativeZ = 0; // yeah I'm all for short names but I'm not calling these vars "cumX".
        startX = startZ = Integer.MAX_VALUE;
        endX = endZ = Integer.MIN_VALUE;

        for(var region : regions) {
            cumulativeX += region.x;
            cumulativeZ += region.z;

            if(region.x < startX) startX = region.x;
            if(region.x > endX) endX = region.x;
            if(region.z < startZ) startZ = region.z;
            if(region.z > endZ) endZ = region.z;
        }

        regionsX = 1 + endX - startX;
        regionsZ = 1 + endZ - startZ;
        grossArea = regionsX * regionsZ;
        centerOfMass = new RegionPos(cumulativeX / regions.size(), cumulativeZ / regions.size());
    }

    public int getStartX() {
        return startX;
    }

    public int getStartZ() {
        return startZ;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndZ() {
        return endZ;
    }

    public int getGrossArea() {
        return grossArea;
    }

    public RegionPos getCenterOfMass() {
        return centerOfMass;
    }

    public void forEach(Consumer<RegionPos> consumer) {
        regions.forEach(consumer);
    }
}
