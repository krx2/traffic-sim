package com.kzebro.trafficsim.memento;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.model.Vehicle;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of the intersection state at a given simulation step.
 */
public record IntersectionMemento(
        Map<Direction, List<Vehicle>> vehicleQueues,
        LightPhase phase,
        int stepsInPhase
) {}
