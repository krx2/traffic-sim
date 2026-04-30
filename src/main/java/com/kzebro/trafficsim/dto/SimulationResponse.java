package com.kzebro.trafficsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Simulation output — one entry per step command")
public record SimulationResponse(
        @Schema(description = "Status of each step in execution order")
        List<StepStatus> stepStatuses
) {}
