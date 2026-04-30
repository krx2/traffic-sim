package com.kzebro.trafficsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "List of commands to execute in the simulation")
public record SimulationRequest(
        @Schema(description = "Ordered list of commands (addVehicle | step)")
        List<CommandDto> commands
) {}
