package com.kzebro.trafficsim.model.light;

public class GreenState implements LightState {
    @Override
    public LightColor getColor() {
        return LightColor.GREEN;
    }

    @Override
    public LightState next() {
        return new YellowState();
    }
}
