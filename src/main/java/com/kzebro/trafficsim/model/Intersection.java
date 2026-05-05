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

public class Intersection implements TrafficSubject {

    private final Map<Direction, Road> roads = new EnumMap<>(Direction.class);
    private LightPhase currentPhase = LightPhase.NS_STRAIGHT_RIGHT;
    private int stepsInCurrentPhase = 0;
    private final List<TrafficObserver> observers = new ArrayList<>();

    public Intersection() {
        for (Direction dir : Direction.values()) {
            Map<LaneType, Lane> lanes = new EnumMap<>(LaneType.class);
            for (LaneType laneType : LaneType.values()) {
                TrafficLight light = new TrafficLight(
                        currentPhase.isGreenFor(dir, laneType) ? new GreenState() : new RedState()
                );
                lanes.put(laneType, new Lane(laneType, light));
            }
            roads.put(dir, new Road(dir, lanes));
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

    public void setPhase(LightPhase newPhase) {
        if (this.currentPhase == newPhase) return;

        for (Direction dir : Direction.values()) {
            for (LaneType laneType : LaneType.values()) {
                Lane lane = roads.get(dir).getLane(laneType);
                if (newPhase.isClearance()) {
                    // All-red clearance: skip the yellow warning so every lane is RED this step
                    lane.getTrafficLight().setState(new RedState());
                } else if (currentPhase.isGreenFor(dir, laneType)) {
                    lane.getTrafficLight().setState(new YellowState());
                } else if (newPhase.isGreenFor(dir, laneType)) {
                    lane.getTrafficLight().setState(new GreenState());
                } else {
                    lane.getTrafficLight().setState(new RedState());
                }
            }
        }
        this.currentPhase = newPhase;
        this.stepsInCurrentPhase = 0;
    }

    private void applyPhaseToLights() {
        for (Direction dir : Direction.values()) {
            for (LaneType laneType : LaneType.values()) {
                Lane lane = roads.get(dir).getLane(laneType);
                lane.getTrafficLight().setState(
                        currentPhase.isGreenFor(dir, laneType) ? new GreenState() : new RedState()
                );
            }
        }
    }

    // ---- Step execution ----

    public List<Vehicle> executeStep() {
        notifyObservers();

        List<Vehicle> left = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            Road road = roads.get(dir);
            for (LaneType laneType : LaneType.values()) {
                Lane lane = road.getLane(laneType);
                LightColor color = lane.getTrafficLight().getColor();
                lane.peek().ifPresent(vehicle -> {
                    if (vehicle.driverStrategy().canPass(color)) {
                        lane.dequeue().ifPresent(left::add);
                    }
                });
            }
        }

        // Finalize phase transition: yellow → red
        for (Direction dir : Direction.values()) {
            for (LaneType laneType : LaneType.values()) {
                TrafficLight light = roads.get(dir).getLane(laneType).getTrafficLight();
                if (light.getColor() == LightColor.YELLOW) {
                    light.transition();
                }
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

    public int getLaneQueueSize(Direction direction, LaneType laneType) {
        return roads.get(direction).laneQueueSize(laneType);
    }

    // ---- Memento ----

    public IntersectionMemento saveMemento() {
        Map<Direction, Map<LaneType, List<Vehicle>>> snapshot = new EnumMap<>(Direction.class);
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
