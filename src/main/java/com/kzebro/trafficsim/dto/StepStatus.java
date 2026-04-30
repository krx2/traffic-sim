package com.kzebro.trafficsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a single simulation step")
public record StepStatus(
        @Schema(description = "IDs of vehicles that left the intersection in this step")
        List<String> leftVehicles
) {}
