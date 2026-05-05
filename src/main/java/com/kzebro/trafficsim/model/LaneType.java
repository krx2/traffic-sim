package com.kzebro.trafficsim.model;

public enum LaneType {
    RIGHT_TURN, STRAIGHT, LEFT_TURN;

    /** Determines which lane a vehicle entering from {@code from} road destined for {@code to} road should use. */
    public static LaneType forMovement(Direction from, Direction to) {
        return switch (from) {
            case NORTH -> switch (to) {
                case WEST  -> RIGHT_TURN;
                case SOUTH -> STRAIGHT;
                case EAST  -> LEFT_TURN;
                default    -> STRAIGHT;
            };
            case SOUTH -> switch (to) {
                case EAST  -> RIGHT_TURN;
                case NORTH -> STRAIGHT;
                case WEST  -> LEFT_TURN;
                default    -> STRAIGHT;
            };
            case EAST -> switch (to) {
                case NORTH -> RIGHT_TURN;
                case WEST  -> STRAIGHT;
                case SOUTH -> LEFT_TURN;
                default    -> STRAIGHT;
            };
            case WEST -> switch (to) {
                case SOUTH -> RIGHT_TURN;
                case EAST  -> STRAIGHT;
                case NORTH -> LEFT_TURN;
                default    -> STRAIGHT;
            };
        };
    }
}
