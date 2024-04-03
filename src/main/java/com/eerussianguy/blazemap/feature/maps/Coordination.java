package com.eerussianguy.blazemap.feature.maps;

public class Coordination {
    public int blockX, blockZ;
    public int chunkX, chunkZ;
    public int regionX, regionZ;

    public int blockPixelX, blockPixelY, blockPixels;
    public int chunkPixelX, chunkPixelY, chunkPixels;
    public int regionPixelX, regionPixelY, regionPixels;

    public void calculate(int mouseX, int mouseY, int beginX, int beginZ, double zoom) {
        blockX = (int) (beginX + (mouseX / zoom));
        blockZ = (int) (beginZ + (mouseY / zoom));
        chunkX = blockX >> 4;
        chunkZ = blockZ >> 4;
        regionX = chunkX >> 5;
        regionZ = chunkZ >> 5;

        if(zoom > 1) {
            blockPixels = (int) zoom;
            blockPixelX = (mouseX / blockPixels) * blockPixels;
            blockPixelY = (mouseY / blockPixels) * blockPixels;
            chunkPixels = blockPixels << 4;
        } else {
            blockPixels = 1;
            blockPixelX = mouseX;
            blockPixelY = mouseY;
            chunkPixels = (int) ((blockPixels << 4) * zoom);
        }

        int blockDistX = blockX & 0b001111;
        int blockDistZ = blockZ & 0b001111;
        int chunkDistX = chunkX & 0b011111;
        int chunkDistZ = chunkZ & 0b011111;

        regionPixels = chunkPixels << 5;
        chunkPixelX = blockPixelX - (int)(blockDistX * zoom);
        chunkPixelY = blockPixelY - (int)(blockDistZ * zoom);
        regionPixelX = chunkPixelX - chunkDistX * chunkPixels;
        regionPixelY = chunkPixelY - chunkDistZ * chunkPixels;
    }
}