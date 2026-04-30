package com.kzebro.trafficsim.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Direction {
    NORTH, SOUTH, EAST, WEST;

    @JsonCreator
    public static Direction fromString(String value) {
        return Direction.valueOf(value.toUpperCase());
    }

    public boolean isNorthSouth() {
        return this == NORTH || this == SOUTH;
    }
}
