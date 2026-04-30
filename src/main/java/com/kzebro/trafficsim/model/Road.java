package com.kzebro.trafficsim.model;

import com.kzebro.trafficsim.model.light.TrafficLight;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class Road {

    private final Direction direction;
    private final TrafficLight trafficLight;
    private final Deque<Vehicle> queue = new ArrayDeque<>();

    public Road(Direction direction, TrafficLight trafficLight) {
        this.direction = direction;
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

    public Direction getDirection() {
        return direction;
    }

    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    /** Returns a snapshot of current vehicle IDs for Memento. */
    public List<Vehicle> snapshotVehicles() {
        return new ArrayList<>(queue);
    }

    /** Restores vehicle queue from a Memento snapshot. */
    public void restoreVehicles(List<Vehicle> vehicles) {
        queue.clear();
        queue.addAll(vehicles);
    }
}
