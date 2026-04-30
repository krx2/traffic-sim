package com.kzebro.trafficsim.command;

import com.kzebro.trafficsim.dto.StepStatus;
import com.kzebro.trafficsim.model.Intersection;

import java.util.Optional;

public interface SimulationCommand {
    /**
     * Executes the command against the intersection.
     * Returns a StepStatus only for step commands.
     */
    Optional<StepStatus> execute(Intersection intersection);
}
