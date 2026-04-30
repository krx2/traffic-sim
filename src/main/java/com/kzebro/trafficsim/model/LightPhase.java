package com.kzebro.trafficsim.model;

public enum LightPhase {
    NS_GREEN, EW_GREEN;

    public boolean isGreenFor(Direction direction) {
        return switch (this) {
            case NS_GREEN -> direction.isNorthSouth();
            case EW_GREEN -> !direction.isNorthSouth();
        };
    }

    public LightPhase opposite() {
        return this == NS_GREEN ? EW_GREEN : NS_GREEN;
    }
}
