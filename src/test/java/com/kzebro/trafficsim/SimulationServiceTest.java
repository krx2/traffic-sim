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
        // vehicle1: south→north = SOUTH STRAIGHT (NS_STRAIGHT_RIGHT = GREEN)
        // vehicle2: north→south = NORTH STRAIGHT (NS_STRAIGHT_RIGHT = GREEN)
        // vehicle3/4: west→south = WEST RIGHT_TURN (EW_STRAIGHT_RIGHT = GREEN)
        var request = new SimulationRequest(List.of(
                addVehicle("vehicle1", "south", "north"),
                addVehicle("vehicle2", "north", "south"),
                step(),  // step 1: NS phase → vehicle1 + vehicle2 leave
                step(),  // step 2: NS empty, phase switches to EW; no vehicles yet
                addVehicle("vehicle3", "west", "south"),
                addVehicle("vehicle4", "west", "south"),
                step(),  // step 3: EW phase → vehicle3 leaves
                step()   // step 4: vehicle4 leaves
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
        // All go NORTH→SOUTH = STRAIGHT lane, GREEN in NS_STRAIGHT_RIGHT
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
    void vehiclesOnNsRoads_nsPhaseReleasesThemImmediately() {
        // n1: north→south = STRAIGHT (GREEN in initial NS_STRAIGHT_RIGHT)
        // s1: south→north = STRAIGHT (GREEN in initial NS_STRAIGHT_RIGHT)
        // e1: east→west   = STRAIGHT (RED initially)
        // w1: west→east   = STRAIGHT (RED initially)
        var request = new SimulationRequest(List.of(
                addVehicle("n1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("s1", "south", "north", DriverStrategyType.PASSIVE),
                addVehicle("e1", "east",  "west",  DriverStrategyType.PASSIVE),
                addVehicle("w1", "west",  "east",  DriverStrategyType.PASSIVE),
                step()
        ));

        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        // NS has vehicles → stays in NS_STRAIGHT_RIGHT → n1 and s1 leave
        assertThat(steps.get(0).leftVehicles())
                .containsExactlyInAnyOrder("n1", "s1");
    }

    @Test
    void fixedTimeStrategy_cyclesThroughPhases() {
        // FixedTimeStrategy(1): NS_STRAIGHT_RIGHT → NS_LEFT → NS_EW_CLEARANCE → EW_STRAIGHT_RIGHT → EW_LEFT → …
        // ns1: NORTH→SOUTH = STRAIGHT = GREEN in NS_STRAIGHT_RIGHT
        // ew1: WEST→EAST   = STRAIGHT = GREEN in EW_STRAIGHT_RIGHT (phase 4)
        var request = new SimulationRequest(List.of(
                addVehicle("ns1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("ew1", "west",  "east",  DriverStrategyType.PASSIVE),
                step(), // 0 steps → keep NS_STRAIGHT_RIGHT → ns1 leaves; stepsInPhase=1
                step(), // 1≥1 → NS_LEFT (YELLOW→RED for NS straight); nothing leaves; stepsInPhase=1
                step(), // 1≥1 → NS_EW_CLEARANCE (all RED); nothing leaves; stepsInPhase=1
                step(), // clearance 1≥1 → EW_STRAIGHT_RIGHT → ew1 leaves; stepsInPhase=1
                step()  // 1≥1 → EW_LEFT; nothing leaves
        ));

        var steps = service.run(request, new FixedTimeStrategy(1)).stepStatuses();

        assertThat(steps.get(0).leftVehicles()).containsExactly("ns1");
        assertThat(steps.get(1).leftVehicles()).isEmpty(); // NS_LEFT
        assertThat(steps.get(2).leftVehicles()).isEmpty(); // NS_EW_CLEARANCE (all red)
        assertThat(steps.get(3).leftVehicles()).containsExactly("ew1");
        assertThat(steps.get(4).leftVehicles()).isEmpty(); // EW_LEFT
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
    void safetyInvariant_nsAndEwNeverBothGreenSimultaneously() {
        // After every step, at most one axis has non-RED straight lanes
        // (aggressive drivers on yellow don't count as conflicting green)
        var request = new SimulationRequest(List.of(
                addVehicle("n1", "north", "south", DriverStrategyType.PASSIVE),
                addVehicle("w1", "west",  "east",  DriverStrategyType.PASSIVE),
                addVehicle("w2", "west",  "east",  DriverStrategyType.PASSIVE),
                step(), step(), step(), step()
        ));

        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        steps.forEach(s -> {
            boolean hasNs = s.leftVehicles().contains("n1");
            boolean hasEw = s.leftVehicles().stream().anyMatch(id -> id.startsWith("w"));
            // PASSIVE n1 cannot pass yellow → if n1 exits it was on GREEN → no EW green conflict
            if (hasNs) {
                assertThat(hasEw)
                        .as("PASSIVE NS vehicle must not exit same step as EW vehicles")
                        .isFalse();
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
