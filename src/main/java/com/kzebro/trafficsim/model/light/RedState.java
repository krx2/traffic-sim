package com.kzebro.trafficsim.model.light;

public class RedState implements LightState {
    @Override
    public LightColor getColor() {
        return LightColor.RED;
    }

    @Override
    public LightState next() {
        return new GreenState();
    }
}
