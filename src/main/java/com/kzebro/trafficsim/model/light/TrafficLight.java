package com.kzebro.trafficsim.model.light;

public class TrafficLight {

    private LightState state;

    public TrafficLight(LightState initialState) {
        this.state = initialState;
    }

    public LightColor getColor() {
        return state.getColor();
    }

    /** Advances the light through its natural cycle (GREEN → YELLOW → RED → GREEN). */
    public void transition() {
        this.state = state.next();
    }

    /** Sets the light to a specific state (used by the intersection controller when switching phases). */
    public void setState(LightState state) {
        this.state = state;
    }

    public boolean isGreen() {
        return state.getColor() == LightColor.GREEN;
    }
}
