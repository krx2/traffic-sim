package com.kzebro.trafficsim.memento;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.LaneType;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.model.Vehicle;

import java.util.List;
import java.util.Map;

public record IntersectionMemento(
        Map<Direction, Map<LaneType, List<Vehicle>>> vehicleQueues,
        LightPhase phase,
        int stepsInPhase
) {}
