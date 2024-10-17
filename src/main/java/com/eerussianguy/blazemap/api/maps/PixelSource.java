package com.eerussianguy.blazemap.api.maps;

public interface PixelSource {
    int getPixel(int x, int y);
    int getWidth();
    int getHeight();
}
