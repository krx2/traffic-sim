package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.light.LightColor;

/** Runs yellow — moves on GREEN and YELLOW. */
public class AggressiveDriver implements DriverStrategy {

    @Override
    public boolean canPass(LightColor color) {
        return color == LightColor.GREEN || color == LightColor.YELLOW;
    }
}
