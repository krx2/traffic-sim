package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.light.LightColor;

/** Stops at yellow — only moves on GREEN. */
public class PassiveDriver implements DriverStrategy {

    @Override
    public boolean canPass(LightColor color) {
        return color == LightColor.GREEN;
    }
}
