package com.kzebro.trafficsim.service;

import com.kzebro.trafficsim.command.SimulationCommand;
import com.kzebro.trafficsim.dto.SimulationRequest;
import com.kzebro.trafficsim.dto.SimulationResponse;
import com.kzebro.trafficsim.dto.StepStatus;
import com.kzebro.trafficsim.factory.CommandFactory;
import com.kzebro.trafficsim.memento.SimulationHistory;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.command.StepCommand;
import com.kzebro.trafficsim.strategy.OptimizationStrategy;
import com.kzebro.trafficsim.strategy.WeightedQueueStrategy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SimulationService {

    private final CommandFactory commandFactory;

    public SimulationService(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    /**
     * Runs a full simulation from the provided command list.
     * Each call creates a fresh intersection — the service is stateless.
     *
     * Flow per command:
     *   - addVehicle → enqueue vehicle on the target road
     *   - step       → ask strategy for the optimal phase, apply it, then move vehicles
     */
    public SimulationResponse run(SimulationRequest request) {
        return run(request, new WeightedQueueStrategy());
    }

    public SimulationResponse run(SimulationRequest request, OptimizationStrategy strategy) {
        Intersection intersection = new Intersection();
        SimulationHistory history = new SimulationHistory();
        intersection.addObserver(history);   // Observer wires Memento to each step

        List<StepStatus> stepStatuses = new ArrayList<>();

        for (var dto : request.commands()) {
            SimulationCommand command = commandFactory.create(dto);

            if (command instanceof StepCommand) {
                LightPhase optimal = strategy.determinePhase(intersection);
                intersection.setPhase(optimal);
            }

            command.execute(intersection).ifPresent(stepStatuses::add);
        }

        return new SimulationResponse(stepStatuses);
    }
}
