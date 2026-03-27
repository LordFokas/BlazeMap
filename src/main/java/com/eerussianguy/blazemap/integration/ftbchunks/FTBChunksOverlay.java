package com.eerussianguy.blazemap.integration.ftbchunks;

import java.util.EnumMap;
import java.util.HashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.eerussianguy.blazemap.BlazeMap;
import com.eerussianguy.blazemap.api.BlazeMapReferences;
import com.eerussianguy.blazemap.api.maps.Overlay;
import com.eerussianguy.blazemap.api.maps.PixelSource;
import com.eerussianguy.blazemap.api.maps.TileResolution;
import com.eerussianguy.blazemap.api.util.RegionPos;
import com.eerussianguy.blazemap.lib.Colors;
import com.eerussianguy.blazemap.lib.Helpers;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.data.Team;

public class FTBChunksOverlay extends Overlay {
    private final EnumMap<TileResolution, HashMap<ResourceKey<Level>, FTBChunksPixelSource>> SOURCES = new EnumMap<>(TileResolution.class);

    public FTBChunksOverlay() {
        super(
            BlazeMapReferences.Overlays.FTBCHUNKS,
            Helpers.translate("blazemap.ftbchunks"),
            BlazeMap.resource("textures/map_icons/overlay_ftbchunks.png")
        );
    }

    @Override
    public PixelSource getPixelSource(ResourceKey<Level> dimension, RegionPos region, TileResolution resolution) {
        return SOURCES
            .computeIfAbsent(resolution, $ -> new HashMap<>())
            .computeIfAbsent(dimension, $ -> new FTBChunksPixelSource(dimension, resolution))
            .at(region);
    }

    private static class FTBChunksPixelSource implements PixelSource {
        private static final int REGION_CHUNKS = 32; // chunks in a region
        private static final int CHUNK_OFFSET = 1; // depth of chunks from neighboring regions to lookup for borders
        private static final int CHUNK_TOTAL = REGION_CHUNKS + CHUNK_OFFSET*2; // total width of chunk matrix (34 x 34)
        private static final byte ALPHA_CENTER = (byte) 0x7F;
        private static final byte ALPHA_BORDER = (byte) 0xFF;

        // Here be binary flags. These represent what edges of the chunk the current pixel touches.
        private static final byte NO_EDGE  = 0b0000;
        private static final byte EDGE_X0  = 0b0001;
        private static final byte EDGE_X1  = 0b0010;
        private static final byte EDGE_Z0  = 0b0100;
        private static final byte EDGE_Z1  = 0b1000;
        private static final byte EDGE_ALL = 0b1111;

        // fast access shortcut to check neighbor chunks for claims based on pixel edge binary flags
        private static final int[][][] EDGE_CHECKS = new int[16][][];
        static {
            int[] x0  = {-1, 0}, x1  = { 1, 0}, z0  = { 0,-1}, z1  = { 0, 1}; // face coordinades
            int[] c00 = {-1,-1}, c01 = {-1, 1}, c10 = { 1,-1}, c11 = { 1, 1}; // corner coordinates

            // non-edge pixels
            EDGE_CHECKS[NO_EDGE] = new int[][]{};

            // face pixels
            EDGE_CHECKS[EDGE_X0] = new int[][]{ x0 };
            EDGE_CHECKS[EDGE_X1] = new int[][]{ x1 };
            EDGE_CHECKS[EDGE_Z0] = new int[][]{ z0 };
            EDGE_CHECKS[EDGE_Z1] = new int[][]{ z1 };

            // corner pixels
            EDGE_CHECKS[EDGE_X0 | EDGE_Z0] = new int[][]{ x0, z0, c00 };
            EDGE_CHECKS[EDGE_X0 | EDGE_Z1] = new int[][]{ x0, z1, c01 };
            EDGE_CHECKS[EDGE_X1 | EDGE_Z0] = new int[][]{ x1, z0, c10 };
            EDGE_CHECKS[EDGE_X1 | EDGE_Z1] = new int[][]{ x1, z1, c11 };

            // extreme case, at 1:16 entire chunk would be a single pixel, touching every edge
            EDGE_CHECKS[EDGE_ALL] = new int[][]{x0, x1, z0, z1, c00, c01, c10, c11};
        }

        private final Team[][] claims = new Team[CHUNK_TOTAL][CHUNK_TOTAL];
        private final ResourceKey<Level> dimension;
        private final TileResolution resolution;

        private FTBChunksPixelSource(ResourceKey<Level> dimension, TileResolution resolution) {
            this.resolution = resolution;
            this.dimension = dimension;
        }

        /** Set this source to a specific region. Causes it to cache all claims in advance. */
        private FTBChunksPixelSource at(RegionPos region) {
            int beginX = region.x << 5;
            int beginZ = region.z << 5;

            var manager = FTBChunksAPI.getManager();
            for(int x = -CHUNK_OFFSET; x < REGION_CHUNKS + CHUNK_OFFSET; x++) {
                for(int z = -CHUNK_OFFSET; z < REGION_CHUNKS + CHUNK_OFFSET; z++) {
                    claims[x + CHUNK_OFFSET][z + CHUNK_OFFSET] = getOwner(manager, beginX + x, beginZ + z);
                }
            }

            return this;
        }

        private Team getOwner(ClaimedChunkManager manager, int x, int z) {
            var claim = manager.getChunk(new ChunkDimPos(dimension, x, z));
            if(claim == null) return null;
            return claim.getTeamData().getTeam();
        }

        @Override
        public int getPixel(int x, int z) {
            int chunkX = x / resolution.chunkWidth;
            int chunkZ = z / resolution.chunkWidth;
            Team team = claims[chunkX + CHUNK_OFFSET][chunkZ + CHUNK_OFFSET];

            // Unclaimed, no need to do all the math.
            if(team == null) return 0;

            // Get a 4-bit pack of flags for the edges this pixel touches.
            int edgeFlags = getPixelEdges(x, z);

            // Given the edges our pixel touches, get a set of neighbor coordinates to test.
            // If all checked neighbors are claimed by the same team, this pixel is inside the claim,
            // otherwise it is in the edge of the claim and different opacity must be used.
            var checks = EDGE_CHECKS[edgeFlags];

            boolean isInside = true;
            for(var coords : checks) {
                Team neighbor = claims[chunkX + CHUNK_OFFSET + coords[0]][chunkZ + CHUNK_OFFSET + coords[1]];
                if(!team.equals(neighbor)) {
                    isInside = false;
                    break;
                }
            }

            int color = Colors.abgr(team.getColor());
            return Colors.setAlpha(color, isInside ? ALPHA_CENTER : ALPHA_BORDER);
        }

        /**
         * Gets a 4-bit pack of flags representing the edges of the chunk this pixel touches.
         * Only 10 of the 16 possible values are actually legal, as follows:
         * - No edges (pixel is in the center of the chunk)
         * - One edge (x4)
         * - Two edges (corner pixel) (x4)
         * - All edges (scale is 1:16 and the pixel covers the entire chunk
         * No illegal values can be returned by this function.
         */
        private byte getPixelEdges(int x, int z) {
            // The algorithm after this would produce the same result, this just saves doing the math.
            // There actually isn't a TileResolution of 1:16 YET (at the time of this writing), but future proofing.
            // There is no need to prepare for 1:32 or above because 1:16 is the current hard limit of what the engine can do.
            if(resolution.chunkWidth == 1) return EDGE_ALL;

            int blockX = x % resolution.chunkWidth;
            int blockZ = z % resolution.chunkWidth;
            boolean x0 = blockX == 0, x1 = blockX == resolution.chunkWidth - 1;
            boolean z0 = blockZ == 0, z1 = blockZ == resolution.chunkWidth - 1;

            return (byte) ((x0 ? EDGE_X0 : NO_EDGE) | (x1 ? EDGE_X1 : NO_EDGE) | (z0 ? EDGE_Z0 : NO_EDGE) | (z1 ? EDGE_Z1 : NO_EDGE));
        }

        @Override
        public int getWidth() {
            return resolution.regionWidth;
        }

        @Override
        public int getHeight() {
            return resolution.regionWidth;
        }
    }
}
