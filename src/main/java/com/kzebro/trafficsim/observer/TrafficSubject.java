package com.kzebro.trafficsim.observer;

public interface TrafficSubject {
    void addObserver(TrafficObserver observer);
    void removeObserver(TrafficObserver observer);
    void notifyObservers();
}
