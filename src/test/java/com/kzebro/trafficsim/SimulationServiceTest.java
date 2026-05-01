package com.kzebro.trafficsim;

import com.kzebro.trafficsim.dto.*;
import com.kzebro.trafficsim.factory.CommandFactory;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.service.SimulationService;
import com.kzebro.trafficsim.strategy.FixedTimeStrategy;
import com.kzebro.trafficsim.strategy.WeightedQueueStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationServiceTest {

    private final SimulationService service = new SimulationService(new CommandFactory());

    // ---- Spec example (required output format) ----

    @Test
    void specExample_exactStepStatuses() {
        var request = new SimulationRequest(List.of(
                addVehicle("vehicle1", "south", "north"),
                addVehicle("vehicle2", "north", "south"),
                step(),
                step(),
                addVehicle("vehicle3", "west", "south"),
                addVehicle("vehicle4", "west", "south"),
                step(),
                step()
        ));

        var response = service.run(request);

        assertThat(response.stepStatuses()).hasSize(4);
        assertThat(response.stepStatuses().get(0).leftVehicles())
                .containsExactlyInAnyOrder("vehicle1", "vehicle2");
        assertThat(response.stepStatuses().get(1).leftVehicles()).isEmpty();
        assertThat(response.stepStatuses().get(2).leftVehicles())
                .containsExactlyInAnyOrder("vehicle3");
        assertThat(response.stepStatuses().get(3).leftVehicles())
                .containsExactlyInAnyOrder("vehicle4");
    }

    // ---- Edge cases ----

    @Test
    void emptyCommandList_returnsEmptyResponse() {
        var response = service.run(new SimulationRequest(Collections.emptyList()));
        assertThat(response.stepStatuses()).isEmpty();
    }

    @Test
    void onlySteps_noVehicles_allStepsEmpty() {
        var request = new SimulationRequest(List.of(step(), step(), step()));
        var response = service.run(request);

        assertThat(response.stepStatuses()).hasSize(3);
        response.stepStatuses().forEach(s -> assertThat(s.leftVehicles()).isEmpty());
    }

    @Test
    void onlyAddVehicles_noSteps_noStatuses() {
        var request = new SimulationRequest(List.of(
                addVehicle("v1", "north", "south"),
                addVehicle("v2", "west", "east")
        ));
        assertThat(service.run(request).stepStatuses()).isEmpty();
    }

    @Test
    void multipleVehiclesSameRoad_fifoOrder() {
        var request = new SimulationRequest(List.of(
                addVehicle("first",  "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("second", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("third",  "north", "south", DriverStrategyType.PASSIVE),
                step(),
                step(),
                step()
        ));

        var steps = service.run(request).stepStatuses();

        assertThat(steps.get(0).leftVehicles()).containsExactly("first");
        assertThat(steps.get(1).leftVehicles()).containsExactly("second");
        assertThat(steps.get(2).leftVehicles()).containsExactly("third");
    }

    @Test
    void vehiclesOnAllFourRoads_nsPhaseReleasesNsOnly() {
        // Initial phase is NS_GREEN → EAST and WEST vehicles wait
        var request = new SimulationRequest(List.of(
                addVehicle("n1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("s1", "south", "north", DriverStrategyType.PASSIVE),
                addVehicle("e1", "east",  "west",  DriverStrategyType.PASSIVE),
                addVehicle("w1", "west",  "east",  DriverStrategyType.PASSIVE),
                step()
        ));

        // NS=2, EW=2 → tie → keep NS_GREEN (current phase, hysteresis)
        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        assertThat(steps.get(0).leftVehicles())
                .as("Only NS vehicles pass when NS_GREEN and loads are tied")
                .containsExactlyInAnyOrder("n1", "s1");
    }

    @Test
    void fixedTimeStrategy_switchesAfterDuration() {
        var request = new SimulationRequest(List.of(
                addVehicle("ns1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("ns2", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("ns3", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("ew1", "west",  "east",  DriverStrategyType.PASSIVE),
                addVehicle("ew2", "west",  "east",  DriverStrategyType.PASSIVE),
                step(), // step 1: NS_GREEN (0 steps → keep), ns1 passes
                step(), // step 2: NS_GREEN (1 step  → keep), ns2 passes
                step(), // step 3: NS_GREEN (2 steps → keep), ns3 passes
                step(), // step 4: 3 steps ≥ duration(3) → switch to EW_GREEN, ew1 passes
                step()  // step 5: EW_GREEN (0 steps → keep), ew2 passes
        ));

        var steps = service.run(request, new FixedTimeStrategy(3)).stepStatuses();

        assertThat(steps.get(0).leftVehicles()).containsExactly("ns1");
        assertThat(steps.get(1).leftVehicles()).containsExactly("ns2");
        assertThat(steps.get(2).leftVehicles()).containsExactly("ns3");
        // Step 4: phase transition NS→EW. N is YELLOW (ew1 not on NORTH, so no yellow effect).
        // EW roads become GREEN → ew1 passes. NS roads become YELLOW (empty queues).
        assertThat(steps.get(3).leftVehicles()).containsExactly("ew1");
        assertThat(steps.get(4).leftVehicles()).containsExactly("ew2");
    }

    @Test
    void stateless_twoCallsProduceSameResult() {
        var request = new SimulationRequest(List.of(
                addVehicle("v1", "north", "south", DriverStrategyType.PASSIVE),
                step()
        ));

        var r1 = service.run(request);
        var r2 = service.run(request);

        assertThat(r1.stepStatuses()).isEqualTo(r2.stepStatuses());
    }

    @Test
    void safetyInvariant_noConflictingGreenLights() {
        // After every step, at most one axis should have GREEN (not both N and E simultaneously)
        var request = new SimulationRequest(List.of(
                addVehicle("n1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("w1", "west",  "east",  DriverStrategyType.PASSIVE),
                addVehicle("w2", "west",  "east",  DriverStrategyType.PASSIVE),
                addVehicle("w3", "west",  "east",  DriverStrategyType.PASSIVE),
                step(), step(), step(), step()
        ));

        // We verify indirectly: only NS or EW vehicles leave per step, never both simultaneously
        // (yellow road might release aggressive drivers, but that's a different road, not conflicting)
        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        steps.forEach(s -> {
            boolean hasNs = s.leftVehicles().contains("n1");
            boolean hasEw = s.leftVehicles().stream().anyMatch(id -> id.startsWith("w"));
            // It's OK to have both when yellow transition (n1 aggressive on yellow + EW green),
            // but n1 here is PASSIVE → can't pass yellow → never conflicts with EW green
            if (hasNs) {
                assertThat(hasEw).as("PASSIVE north vehicle must not exit same step as EW vehicles (no yellow pass)").isFalse();
            }
        });
    }

    // ---- Builder helpers ----

    private AddVehicleCommandDto addVehicle(String id, String start, String end) {
        return new AddVehicleCommandDto(id, Direction.fromString(start), Direction.fromString(end), null);
    }

    private AddVehicleCommandDto addVehicle(String id, String start, String end, DriverStrategyType strategy) {
        return new AddVehicleCommandDto(id, Direction.fromString(start), Direction.fromString(end), strategy);
    }

    private StepCommandDto step() {
        return new StepCommandDto();
    }
}
