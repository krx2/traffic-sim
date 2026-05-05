package com.kzebro.trafficsim.model;

/**
 * Six-phase traffic light cycle:
 * 1. NS_STRAIGHT_RIGHT  – N/S: STRAIGHT and RIGHT_TURN green
 * 2. NS_LEFT            – N/S: LEFT_TURN green (skipped when no vehicles)
 * 3. NS_EW_CLEARANCE    – all lanes RED (1-step buffer before EW direction)
 * 4. EW_STRAIGHT_RIGHT  – E/W: STRAIGHT and RIGHT_TURN green
 * 5. EW_LEFT            – E/W: LEFT_TURN green (skipped when no vehicles)
 * 6. EW_NS_CLEARANCE    – all lanes RED (1-step buffer before NS direction)
 */
public enum LightPhase {
    NS_STRAIGHT_RIGHT, NS_LEFT, NS_EW_CLEARANCE,
    EW_STRAIGHT_RIGHT, EW_LEFT, EW_NS_CLEARANCE;

    public boolean isGreenFor(Direction direction, LaneType laneType) {
        return switch (this) {
            case NS_STRAIGHT_RIGHT -> direction.isNorthSouth() && laneType != LaneType.LEFT_TURN;
            case NS_LEFT           -> direction.isNorthSouth() && laneType == LaneType.LEFT_TURN;
            case EW_STRAIGHT_RIGHT -> !direction.isNorthSouth() && laneType != LaneType.LEFT_TURN;
            case EW_LEFT           -> !direction.isNorthSouth() && laneType == LaneType.LEFT_TURN;
            case NS_EW_CLEARANCE, EW_NS_CLEARANCE -> false;
        };
    }

    /** Next phase in the fixed 6-phase sequence (wraps around). */
    public LightPhase next() {
        return switch (this) {
            case NS_STRAIGHT_RIGHT -> NS_LEFT;
            case NS_LEFT           -> NS_EW_CLEARANCE;
            case NS_EW_CLEARANCE   -> EW_STRAIGHT_RIGHT;
            case EW_STRAIGHT_RIGHT -> EW_LEFT;
            case EW_LEFT           -> EW_NS_CLEARANCE;
            case EW_NS_CLEARANCE   -> NS_STRAIGHT_RIGHT;
        };
    }

    public boolean isNorthSouth() {
        return this == NS_STRAIGHT_RIGHT || this == NS_LEFT;
    }

    /** Clearance phases enforce all-red for one step between direction changes. */
    public boolean isClearance() {
        return this == NS_EW_CLEARANCE || this == EW_NS_CLEARANCE;
    }
}
