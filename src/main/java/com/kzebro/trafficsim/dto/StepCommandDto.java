package com.kzebro.trafficsim.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Advances the simulation by one step")
public record StepCommandDto() implements CommandDto {}
