package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.light.LightColor;

public interface DriverStrategy {
    boolean canPass(LightColor color);
}
