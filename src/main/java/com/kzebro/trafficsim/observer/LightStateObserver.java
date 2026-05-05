package com.kzebro.trafficsim.observer;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LaneType;
import com.kzebro.trafficsim.model.light.LightColor;

import java.util.EnumMap;
import java.util.Map;

public class LightStateObserver implements TrafficObserver {

    private Map<Direction, Map<LaneType, LightColor>> lastSnapshot = new EnumMap<>(Direction.class);

    @Override
    public void onStepCompleted(Intersection intersection) {
        Map<Direction, Map<LaneType, LightColor>> snapshot = new EnumMap<>(Direction.class);
        intersection.getRoads().forEach((dir, road) -> {
            Map<LaneType, LightColor> laneColors = new EnumMap<>(LaneType.class);
            road.getLanes().forEach((laneType, lane) ->
                    laneColors.put(laneType, lane.getTrafficLight().getColor()));
            snapshot.put(dir, Map.copyOf(laneColors));
        });
        this.lastSnapshot = snapshot;
    }

    public Map<Direction, Map<LaneType, LightColor>> getSnapshot() {
        return Map.copyOf(lastSnapshot);
    }
}
