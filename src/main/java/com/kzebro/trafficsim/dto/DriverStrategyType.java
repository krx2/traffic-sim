package com.kzebro.trafficsim.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DriverStrategyType {
    PASSIVE, AGGRESSIVE;

    @JsonCreator
    public static DriverStrategyType fromString(String value) {
        if (value == null) return null;
        return DriverStrategyType.valueOf(value.toUpperCase());
    }
}
