package com.kzebro.trafficsim.model;

import com.kzebro.trafficsim.strategy.DriverStrategy;

public record Vehicle(String id, Direction startRoad, Direction endRoad, DriverStrategy driverStrategy) {}
