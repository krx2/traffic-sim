package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;

/**
 * Switches the active phase every {@code phaseDuration} steps regardless of queue load.
 */
public class FixedTimeStrategy implements OptimizationStrategy {

    private final int phaseDuration;

    public FixedTimeStrategy(int phaseDuration) {
        this.phaseDuration = phaseDuration;
    }

    public FixedTimeStrategy() {
        this(3);
    }

    @Override
    public LightPhase determinePhase(Intersection intersection) {
        if (intersection.getStepsInCurrentPhase() >= phaseDuration) {
            return intersection.getCurrentPhase().opposite();
        }
        return intersection.getCurrentPhase();
    }
}
