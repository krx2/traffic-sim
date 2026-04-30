package com.kzebro.trafficsim.observer;

import com.kzebro.trafficsim.model.Intersection;

@FunctionalInterface
public interface TrafficObserver {
    void onStepCompleted(Intersection intersection);
}
