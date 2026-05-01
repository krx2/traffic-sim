package com.kzebro.trafficsim.model;

import com.kzebro.trafficsim.memento.IntersectionMemento;
import com.kzebro.trafficsim.model.light.GreenState;
import com.kzebro.trafficsim.model.light.LightColor;
import com.kzebro.trafficsim.model.light.RedState;
import com.kzebro.trafficsim.model.light.TrafficLight;
import com.kzebro.trafficsim.model.light.YellowState;
import com.kzebro.trafficsim.observer.TrafficObserver;
import com.kzebro.trafficsim.observer.TrafficSubject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Composite root: holds four Roads (N/S/E/W), manages the active LightPhase,
 * and acts as the Subject in the Observer pattern.
 */
public class Intersection implements TrafficSubject {

    private final Map<Direction, Road> roads = new EnumMap<>(Direction.class);
    private LightPhase currentPhase = LightPhase.NS_GREEN;
    private int stepsInCurrentPhase = 0;
    private final List<TrafficObserver> observers = new ArrayList<>();

    public Intersection() {
        for (Direction dir : Direction.values()) {
            TrafficLight light = new TrafficLight(
                    currentPhase.isGreenFor(dir) ? new GreenState() : new RedState()
            );
            roads.put(dir, new Road(dir, light));
        }
    }

    // ---- TrafficSubject ----

    @Override
    public void addObserver(TrafficObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(TrafficObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        observers.forEach(o -> o.onStepCompleted(this));
    }

    // ---- Phase management ----

    /**
     * Switches the active phase.
     * Outgoing roads (were GREEN) transition to YELLOW so aggressive drivers
     * can still clear the intersection this step; incoming roads go GREEN immediately.
     * YELLOW lights are finalized to RED at the end of {@link #executeStep()}.
     */
    public void setPhase(LightPhase phase) {
        if (this.currentPhase == phase) return;

        for (Direction dir : Direction.values()) {
            roads.get(dir).getTrafficLight().setState(
                    currentPhase.isGreenFor(dir) ? new YellowState() : new GreenState()
            );
        }
        this.currentPhase = phase;
        this.stepsInCurrentPhase = 0;
    }

    /** Used for clean restoration (no yellow transition needed). */
    private void applyPhaseToLights() {
        for (Direction dir : Direction.values()) {
            roads.get(dir).getTrafficLight().setState(
                    currentPhase.isGreenFor(dir) ? new GreenState() : new RedState()
            );
        }
    }

    // ---- Step execution ----

    /**
     * Advances one simulation step.
     * Each road's front vehicle passes only if its {@code DriverStrategy.canPass(lightColor)} returns true:
     * - GREEN  → both passive and aggressive drivers pass
     * - YELLOW → only aggressive drivers pass (passive stop)
     * - RED    → nobody passes
     *
     * After vehicles move, any YELLOW light finalizes to RED (completing the phase transition).
     */
    public List<Vehicle> executeStep() {
        // Notify observers with pre-movement state so Memento captures the snapshot
        // that can be used to undo this step (i.e. restore to exactly before vehicles moved).
        notifyObservers();

        List<Vehicle> left = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            Road road = roads.get(dir);
            LightColor color = road.getTrafficLight().getColor();
            road.peek().ifPresent(vehicle -> {
                if (vehicle.driverStrategy().canPass(color)) {
                    road.dequeue().ifPresent(left::add);
                }
            });
        }

        // Finalize phase transition: yellow → red
        for (Direction dir : Direction.values()) {
            TrafficLight light = roads.get(dir).getTrafficLight();
            if (light.getColor() == LightColor.YELLOW) {
                light.transition();
            }
        }

        stepsInCurrentPhase++;
        return left;
    }

    // ---- Vehicle management ----

    public void addVehicle(Vehicle vehicle) {
        roads.get(vehicle.startRoad()).enqueue(vehicle);
    }

    public int getQueueSize(Direction direction) {
        return roads.get(direction).queueSize();
    }

    // ---- Memento ----

    public IntersectionMemento saveMemento() {
        Map<Direction, List<Vehicle>> snapshot = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            snapshot.put(dir, roads.get(dir).snapshotVehicles());
        }
        return new IntersectionMemento(snapshot, currentPhase, stepsInCurrentPhase);
    }

    public void restoreMemento(IntersectionMemento memento) {
        this.currentPhase = memento.phase();
        this.stepsInCurrentPhase = memento.stepsInPhase();
        for (Direction dir : Direction.values()) {
            roads.get(dir).restoreVehicles(memento.vehicleQueues().get(dir));
        }
        applyPhaseToLights();
    }

    // ---- Getters ----

    public LightPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getStepsInCurrentPhase() {
        return stepsInCurrentPhase;
    }

    public Map<Direction, Road> getRoads() {
        return roads;
    }
}
