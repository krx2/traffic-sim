package com.kzebro.trafficsim.memento;

import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.observer.TrafficObserver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Caretaker in the Memento pattern and Observer of Intersection steps.
 * Saves a snapshot after every step; supports single-level undo.
 */
public class SimulationHistory implements TrafficObserver {

    private final Deque<IntersectionMemento> history = new ArrayDeque<>();

    @Override
    public void onStepCompleted(Intersection intersection) {
        history.push(intersection.saveMemento());
    }

    public Optional<IntersectionMemento> undo(Intersection intersection) {
        if (history.isEmpty()) return Optional.empty();
        IntersectionMemento memento = history.pop();
        intersection.restoreMemento(memento);
        return Optional.of(memento);
    }

    public Optional<IntersectionMemento> peek() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.peek());
    }

    public int size() {
        return history.size();
    }
}
