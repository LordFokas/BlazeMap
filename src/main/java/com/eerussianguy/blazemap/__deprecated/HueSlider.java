package com.eerussianguy.blazemap.__deprecated;

import java.util.function.Consumer;

import net.minecraftforge.client.gui.widget.ForgeSlider;

@Deprecated
public class HueSlider extends ForgeSlider {
    private Consumer<Double> responder;

    public HueSlider(int x, int y, int width, int height, double minValue, double maxValue, double currentValue, double stepSize) {
        super(x, y, width, height, null, null, minValue, maxValue, currentValue, stepSize, 2, false);
    }

    public void setResponder(Consumer<Double> responder) {
        this.responder = responder;
    }

    @Override
    protected void applyValue() {
        if(this.responder != null) {
            responder.accept(this.value * 360);
        }
    }
}