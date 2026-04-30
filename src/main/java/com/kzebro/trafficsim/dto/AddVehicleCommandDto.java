package com.kzebro.trafficsim.dto;

import com.kzebro.trafficsim.model.Direction;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Adds a vehicle to a starting road heading towards an end road")
public record AddVehicleCommandDto(
        @Schema(example = "vehicle1") String vehicleId,
        @Schema(example = "south") Direction startRoad,
        @Schema(example = "north") Direction endRoad,
        @Schema(description = "PASSIVE (stops at yellow) or AGGRESSIVE (runs yellow). Omit for random 50/50.", example = "PASSIVE", nullable = true)
        DriverStrategyType driverStrategy
) implements CommandDto {}
