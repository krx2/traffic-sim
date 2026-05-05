package com.kzebro.trafficsim.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Road {

    private final Direction direction;
    private final Map<LaneType, Lane> lanes;

    public Road(Direction direction, Map<LaneType, Lane> lanes) {
        this.direction = direction;
        this.lanes = new EnumMap<>(lanes);
    }

    /** Routes the vehicle to the correct lane based on its destination. */
    public void enqueue(Vehicle vehicle) {
        LaneType type = LaneType.forMovement(direction, vehicle.endRoad());
        lanes.get(type).enqueue(vehicle);
    }

    public Lane getLane(LaneType type) {
        return lanes.get(type);
    }

    public Map<LaneType, Lane> getLanes() {
        return lanes;
    }

    public int queueSize() {
        return lanes.values().stream().mapToInt(Lane::queueSize).sum();
    }

    public int laneQueueSize(LaneType type) {
        return lanes.get(type).queueSize();
    }

    public boolean isEmpty() {
        return lanes.values().stream().allMatch(Lane::isEmpty);
    }

    public Direction getDirection() {
        return direction;
    }

    public Map<LaneType, List<Vehicle>> snapshotVehicles() {
        Map<LaneType, List<Vehicle>> snapshot = new EnumMap<>(LaneType.class);
        for (LaneType type : LaneType.values()) {
            snapshot.put(type, lanes.get(type).snapshotVehicles());
        }
        return snapshot;
    }

    public void restoreVehicles(Map<LaneType, List<Vehicle>> snapshot) {
        for (LaneType type : LaneType.values()) {
            lanes.get(type).restoreVehicles(snapshot.getOrDefault(type, List.of()));
        }
    }
}
