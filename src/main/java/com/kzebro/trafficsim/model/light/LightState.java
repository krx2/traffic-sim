package com.kzebro.trafficsim.model.light;

public interface LightState {
    LightColor getColor();
    LightState next();
}
