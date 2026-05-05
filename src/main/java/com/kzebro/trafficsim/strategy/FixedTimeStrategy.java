package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;

/**
 * Advances to the next phase every {@code phaseDuration} steps, cycling through all six phases.
 * Clearance phases always last exactly one step regardless of {@code phaseDuration}.
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
        LightPhase current = intersection.getCurrentPhase();
        int steps = intersection.getStepsInCurrentPhase();

        if (current.isClearance()) {
            return steps >= 1 ? current.next() : current;
        }
        return steps >= phaseDuration ? current.next() : current;
    }
}
