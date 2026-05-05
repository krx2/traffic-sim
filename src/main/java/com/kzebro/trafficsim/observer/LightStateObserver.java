package com.kzebro.trafficsim.observer;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.light.LightColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Captures the traffic light color for every road at the moment each step begins
 * (before vehicles move, so the displayed colors match what drivers actually saw).
 */
public class LightStateObserver implements TrafficObserver {

    private Map<Direction, LightColor> lastSnapshot = new EnumMap<>(Direction.class);

    @Override
    public void onStepCompleted(Intersection intersection) {
        Map<Direction, LightColor> snapshot = new EnumMap<>(Direction.class);
        intersection.getRoads().forEach((dir, road) ->
                snapshot.put(dir, road.getTrafficLight().getColor()));
        this.lastSnapshot = snapshot;
    }

    public Map<Direction, LightColor> getSnapshot() {
        return Map.copyOf(lastSnapshot);
    }
}
