package com.kzebro.trafficsim.model.light;

public class YellowState implements LightState {
    @Override
    public LightColor getColor() {
        return LightColor.YELLOW;
    }

    @Override
    public LightState next() {
        return new RedState();
    }
}
