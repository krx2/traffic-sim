package com.kzebro.trafficsim.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddVehicleCommandDto.class, name = "addVehicle"),
        @JsonSubTypes.Type(value = StepCommandDto.class, name = "step")
})
public sealed interface CommandDto permits AddVehicleCommandDto, StepCommandDto {}
