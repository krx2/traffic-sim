package com.kzebro.trafficsim.model;

import com.kzebro.trafficsim.model.light.TrafficLight;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class Lane {

    private final LaneType type;
    private final TrafficLight trafficLight;
    private final Deque<Vehicle> queue = new ArrayDeque<>();

    public Lane(LaneType type, TrafficLight trafficLight) {
        this.type = type;
        this.trafficLight = trafficLight;
    }

    public void enqueue(Vehicle vehicle) {
        queue.addLast(vehicle);
    }

    public Optional<Vehicle> peek() {
        return Optional.ofNullable(queue.peekFirst());
    }

    public Optional<Vehicle> dequeue() {
        return Optional.ofNullable(queue.pollFirst());
    }

    public int queueSize() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public LaneType getType() {
        return type;
    }

    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public List<Vehicle> snapshotVehicles() {
        return new ArrayList<>(queue);
    }

    public void restoreVehicles(List<Vehicle> vehicles) {
        queue.clear();
        queue.addAll(vehicles);
    }
}
