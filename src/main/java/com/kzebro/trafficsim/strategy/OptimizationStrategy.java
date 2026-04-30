package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;

public interface OptimizationStrategy {
    /**
     * Determines which LightPhase should be active before the next step,
     * based on the current state of the intersection.
     */
    LightPhase determinePhase(Intersection intersection);
}
