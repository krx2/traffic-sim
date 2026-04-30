package com.kzebro.trafficsim.command;

import com.kzebro.trafficsim.dto.DriverStrategyType;
import com.kzebro.trafficsim.dto.StepStatus;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.Vehicle;
import com.kzebro.trafficsim.strategy.AggressiveDriver;
import com.kzebro.trafficsim.strategy.DriverStrategy;
import com.kzebro.trafficsim.strategy.PassiveDriver;

import java.util.Optional;
import java.util.random.RandomGenerator;

public class AddVehicleCommand implements SimulationCommand {

    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private final String vehicleId;
    private final Direction startRoad;
    private final Direction endRoad;
    private final DriverStrategy driverStrategy;

    public AddVehicleCommand(String vehicleId, Direction startRoad, Direction endRoad, DriverStrategyType strategyType) {
        this.vehicleId = vehicleId;
        this.startRoad = startRoad;
        this.endRoad = endRoad;
        this.driverStrategy = strategyType == null ? randomStrategy() : resolve(strategyType);
    }

    private static DriverStrategy randomStrategy() {
        return RANDOM.nextBoolean() ? new AggressiveDriver() : new PassiveDriver();
    }

    private static DriverStrategy resolve(DriverStrategyType type) {
        return switch (type) {
            case PASSIVE -> new PassiveDriver();
            case AGGRESSIVE -> new AggressiveDriver();
        };
    }

    @Override
    public Optional<StepStatus> execute(Intersection intersection) {
        intersection.addVehicle(new Vehicle(vehicleId, startRoad, endRoad, driverStrategy));
        return Optional.empty();
    }
}
