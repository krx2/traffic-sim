package com.kzebro.trafficsim.strategy;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LaneType;
import com.kzebro.trafficsim.model.LightPhase;

/**
 * Cycles through the six-phase sequence, inserting an all-red clearance step between
 * direction changes (NS→EW and EW→NS). Each phase is served until its lanes are empty
 * or a maximum step count is reached. Left-turn phases are skipped when no vehicles wait.
 *
 * Sequence: NS_STRAIGHT_RIGHT → NS_LEFT* → NS_EW_CLEARANCE → EW_STRAIGHT_RIGHT → EW_LEFT* → EW_NS_CLEARANCE → repeat
 * (* = skipped if no vehicles in those lanes)
 */
public class WeightedQueueStrategy implements OptimizationStrategy {

    private static final int MAX_STRAIGHT_RIGHT = 10;
    private static final int MAX_LEFT = 5;

    @Override
    public LightPhase determinePhase(Intersection intersection) {
        LightPhase current = intersection.getCurrentPhase();
        int steps = intersection.getStepsInCurrentPhase();

        return switch (current) {
            case NS_STRAIGHT_RIGHT -> {
                int nsLoad = nsLoad(intersection, LaneType.STRAIGHT) + nsLoad(intersection, LaneType.RIGHT_TURN);
                if (nsLoad > 0 && steps < MAX_STRAIGHT_RIGHT) yield current;
                yield nsLoad(intersection, LaneType.LEFT_TURN) > 0
                        ? LightPhase.NS_LEFT : LightPhase.NS_EW_CLEARANCE;
            }
            case NS_LEFT -> {
                if (nsLoad(intersection, LaneType.LEFT_TURN) > 0 && steps < MAX_LEFT) yield current;
                yield LightPhase.NS_EW_CLEARANCE;
            }
            case NS_EW_CLEARANCE, EW_NS_CLEARANCE -> steps >= 1 ? current.next() : current;
            case EW_STRAIGHT_RIGHT -> {
                int ewLoad = ewLoad(intersection, LaneType.STRAIGHT) + ewLoad(intersection, LaneType.RIGHT_TURN);
                if (ewLoad > 0 && steps < MAX_STRAIGHT_RIGHT) yield current;
                yield ewLoad(intersection, LaneType.LEFT_TURN) > 0
                        ? LightPhase.EW_LEFT : LightPhase.EW_NS_CLEARANCE;
            }
            case EW_LEFT -> {
                if (ewLoad(intersection, LaneType.LEFT_TURN) > 0 && steps < MAX_LEFT) yield current;
                yield LightPhase.EW_NS_CLEARANCE;
            }
        };
    }

    private int nsLoad(Intersection i, LaneType type) {
        return i.getLaneQueueSize(Direction.NORTH, type) + i.getLaneQueueSize(Direction.SOUTH, type);
    }

    private int ewLoad(Intersection i, LaneType type) {
        return i.getLaneQueueSize(Direction.EAST, type) + i.getLaneQueueSize(Direction.WEST, type);
    }
}
