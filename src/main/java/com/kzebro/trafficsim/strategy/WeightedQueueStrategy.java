package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;

/**
 * Switches the active phase to whichever axis (N-S or E-W) has more vehicles
 * waiting. Keeps the current phase when loads are tied (hysteresis).
 *
 * Example: if N+S queue = 0 and E+W queue = 2 → switch to EW_GREEN.
 */
public class WeightedQueueStrategy implements OptimizationStrategy {

    @Override
    public LightPhase determinePhase(Intersection intersection) {
        int nsLoad = intersection.getQueueSize(Direction.NORTH)
                + intersection.getQueueSize(Direction.SOUTH);
        int ewLoad = intersection.getQueueSize(Direction.EAST)
                + intersection.getQueueSize(Direction.WEST);

        LightPhase current = intersection.getCurrentPhase();

        if (current == LightPhase.NS_GREEN && ewLoad > nsLoad) return LightPhase.EW_GREEN;
        if (current == LightPhase.EW_GREEN && nsLoad > ewLoad) return LightPhase.NS_GREEN;
        return current;
    }
}
