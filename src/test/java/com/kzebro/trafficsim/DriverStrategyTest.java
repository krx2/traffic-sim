package com.kzebro.trafficsim;

import com.kzebro.trafficsim.command.AddVehicleCommand;
import com.kzebro.trafficsim.dto.*;
import com.kzebro.trafficsim.factory.CommandFactory;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LaneType;
import com.kzebro.trafficsim.model.light.LightColor;
import com.kzebro.trafficsim.service.SimulationService;
import com.kzebro.trafficsim.strategy.AggressiveDriver;
import com.kzebro.trafficsim.strategy.PassiveDriver;
import com.kzebro.trafficsim.strategy.WeightedQueueStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriverStrategyTest {

    private final SimulationService service = new SimulationService(new CommandFactory());

    // ---- PassiveDriver unit tests ----

    @Test
    void passiveDriver_canPassOnGreen() {
        assertThat(new PassiveDriver().canPass(LightColor.GREEN)).isTrue();
    }

    @Test
    void passiveDriver_stopsAtYellow() {
        assertThat(new PassiveDriver().canPass(LightColor.YELLOW)).isFalse();
    }

    @Test
    void passiveDriver_stopsAtRed() {
        assertThat(new PassiveDriver().canPass(LightColor.RED)).isFalse();
    }

    // ---- AggressiveDriver unit tests ----

    @Test
    void aggressiveDriver_canPassOnGreen() {
        assertThat(new AggressiveDriver().canPass(LightColor.GREEN)).isTrue();
    }

    @Test
    void aggressiveDriver_canPassOnYellow() {
        assertThat(new AggressiveDriver().canPass(LightColor.YELLOW)).isTrue();
    }

    @Test
    void aggressiveDriver_stopsAtRed() {
        assertThat(new AggressiveDriver().canPass(LightColor.RED)).isFalse();
    }

    // ---- Random strategy assignment ----

    @Test
    void addVehicleCommand_nullStrategy_assignsNonNull() {
        var request = new SimulationRequest(List.of(
                new AddVehicleCommandDto("v1", Direction.NORTH, Direction.SOUTH, null),
                new StepCommandDto()
        ));
        assertThat(service.run(request).stepStatuses()).hasSize(1);
    }

    @Test
    void addVehicleCommand_nullStrategy_assignsEitherDriverType() {
        var strategies = new java.util.HashSet<String>();
        for (int i = 0; i < 100; i++) {
            var cmd = new AddVehicleCommand("x", Direction.NORTH, Direction.SOUTH, null);
            var tmpIntersection = new Intersection();
            cmd.execute(tmpIntersection);
            // NORTH→SOUTH = STRAIGHT lane
            var vehicle = tmpIntersection.getRoads().get(Direction.NORTH)
                    .getLane(LaneType.STRAIGHT).peek().orElseThrow();
            strategies.add(vehicle.driverStrategy().getClass().getSimpleName());
        }
        assertThat(strategies)
                .as("Both PASSIVE and AGGRESSIVE should appear in 100 random assignments")
                .containsExactlyInAnyOrder("PassiveDriver", "AggressiveDriver");
    }

    // ---- Integration: yellow light scenarios ----

    /**
     * Aggressive driver on NORTH STRAIGHT passes on YELLOW during phase transition.
     * Phase starts NS_STRAIGHT_RIGHT (NORTH STRAIGHT = GREEN, so aggressive passes immediately
     * in step 1 before any switch). We need to place the aggressive in the YELLOW scenario:
     * force phase switch before step.
     */
    @Test
    void aggressiveDriver_passesOnYellow_duringPhaseTransition() {
        // Set up: aggressive on NORTH STRAIGHT, then switch phase so NORTH STRAIGHT → YELLOW
        var request = new SimulationRequest(List.of(
                new AddVehicleCommandDto("aggressive", Direction.NORTH, Direction.SOUTH, DriverStrategyType.AGGRESSIVE),
                new AddVehicleCommandDto("ew1", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew2", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew3", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new StepCommandDto()  // NS has 1, EW has 3: NS still serves first (NS_STRAIGHT_RIGHT kept)
        ));

        // With WeightedQueueStrategy: NS_STRAIGHT_RIGHT has 1 vehicle → keep → aggressive passes GREEN
        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        assertThat(steps.get(0).leftVehicles()).containsExactly("aggressive");
    }

    /**
     * Passive driver stops at YELLOW. After NS_STRAIGHT_RIGHT phase ends,
     * a clearance step (all RED) is inserted before EW goes green.
     */
    @Test
    void passiveDriver_stopsAtYellow_remainsInQueue() {
        var request = new SimulationRequest(List.of(
                new AddVehicleCommandDto("passive", Direction.NORTH, Direction.SOUTH, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew1", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new StepCommandDto(),   // step 1: NS_STRAIGHT_RIGHT → passive leaves
                new StepCommandDto(),   // step 2: NS_EW_CLEARANCE (all RED) → nothing leaves
                new StepCommandDto()    // step 3: EW_STRAIGHT_RIGHT → ew1 leaves
        ));

        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        assertThat(steps.get(0).leftVehicles()).containsExactly("passive");
        assertThat(steps.get(1).leftVehicles()).isEmpty(); // clearance
        assertThat(steps.get(2).leftVehicles()).containsExactly("ew1");
    }

    /**
     * Aggressive driver runs yellow while EW green starts simultaneously.
     * This requires manually constructing the scenario: aggressive on NS STRAIGHT,
     * immediately switch to EW phase → NS STRAIGHT = YELLOW → aggressive can pass.
     */
    @Test
    void aggressivePassesYellow_simultaneouslyWithEwGreen() {
        Intersection intersection = new Intersection();
        intersection.addVehicle(new com.kzebro.trafficsim.model.Vehicle(
                "ag", Direction.NORTH, Direction.SOUTH, new AggressiveDriver()));
        intersection.addVehicle(new com.kzebro.trafficsim.model.Vehicle(
                "ew1", Direction.WEST, Direction.EAST, new PassiveDriver()));

        // Force switch: NS STRAIGHT → YELLOW, EW STRAIGHT → GREEN
        intersection.setPhase(com.kzebro.trafficsim.model.LightPhase.EW_STRAIGHT_RIGHT);

        var left = intersection.executeStep();

        // Aggressive runs yellow; EW passive runs green
        assertThat(left).extracting(com.kzebro.trafficsim.model.Vehicle::id)
                .containsExactlyInAnyOrder("ag", "ew1");
    }
}
