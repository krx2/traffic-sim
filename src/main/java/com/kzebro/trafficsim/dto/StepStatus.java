package com.kzebro.trafficsim.dto;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.light.LightColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Result of a single simulation step")
public record StepStatus(
        @Schema(description = "IDs of vehicles that left the intersection in this step")
        List<String> leftVehicles,
        @Schema(description = "Traffic light color per direction at the moment vehicles moved")
        Map<Direction, LightColor> lights
) {}
