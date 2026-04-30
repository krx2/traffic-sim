package com.kzebro.trafficsim.factory;

import com.kzebro.trafficsim.command.AddVehicleCommand;
import com.kzebro.trafficsim.command.SimulationCommand;
import com.kzebro.trafficsim.command.StepCommand;
import com.kzebro.trafficsim.dto.AddVehicleCommandDto;
import com.kzebro.trafficsim.dto.CommandDto;
import com.kzebro.trafficsim.dto.StepCommandDto;
import org.springframework.stereotype.Component;

@Component
public class CommandFactory {

    public SimulationCommand create(CommandDto dto) {
        return switch (dto) {
            case AddVehicleCommandDto d ->
                    new AddVehicleCommand(d.vehicleId(), d.startRoad(), d.endRoad(), d.driverStrategy());
            case StepCommandDto ignored -> new StepCommand();
        };
    }
}
