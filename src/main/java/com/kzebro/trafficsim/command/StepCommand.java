package com.kzebro.trafficsim.command;

import com.kzebro.trafficsim.dto.StepStatus;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.Vehicle;

import java.util.List;
import java.util.Optional;

public class StepCommand implements SimulationCommand {

    @Override
    public Optional<StepStatus> execute(Intersection intersection) {
        List<String> leftVehicleIds = intersection.executeStep().stream()
                .map(Vehicle::id)
                .toList();
        return Optional.of(new StepStatus(leftVehicleIds, null));
    }
}
