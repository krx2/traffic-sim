package com.kzebro.trafficsim;

import com.kzebro.trafficsim.dto.*;
import com.kzebro.trafficsim.factory.CommandFactory;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Vehicle;
import com.kzebro.trafficsim.model.Intersection;
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
        // Just verify it doesn't throw and produces a result
        assertThat(service.run(request).stepStatuses()).hasSize(1);
    }

    @Test
    void addVehicleCommand_nullStrategy_assignsEitherDriverType() {
        // Run many times to confirm both types can appear (probabilistic guard)
        var seen = new java.util.HashSet<Class<?>>();
        for (int i = 0; i < 50; i++) {
            Intersection intersection = new Intersection();
            intersection.addVehicle(new Vehicle("v", Direction.NORTH, Direction.SOUTH,
                    new com.kzebro.trafficsim.command.AddVehicleCommand(
                            "v", Direction.NORTH, Direction.SOUTH, null) {
                        // we only need the strategy, so call the factory path instead
                    }.execute(intersection) != null  // dummy usage
                            ? new PassiveDriver() : new AggressiveDriver()));
        }
        // The real randomness test: check via multiple AddVehicleCommand instances
        var strategies = new java.util.HashSet<String>();
        for (int i = 0; i < 100; i++) {
            var cmd = new com.kzebro.trafficsim.command.AddVehicleCommand("x", Direction.NORTH, Direction.SOUTH, null);
            var tmpIntersection = new Intersection();
            cmd.execute(tmpIntersection);
            var vehicle = tmpIntersection.getRoads().get(Direction.NORTH).peek().orElseThrow();
            strategies.add(vehicle.driverStrategy().getClass().getSimpleName());
        }
        assertThat(strategies).as("Both PASSIVE and AGGRESSIVE should appear in 100 random assignments")
                .containsExactlyInAnyOrder("PassiveDriver", "AggressiveDriver");
    }

    // ---- Integration: yellow light scenarios ----

    /**
     * EW queue = 3, NS queue = 1 (AGGRESSIVE).
     * Strategy switches to EW_GREEN → NORTH gets YELLOW.
     * AGGRESSIVE runs the yellow → appears in step 1 alongside ew-1.
     */
    @Test
    void aggressivePassesYellow_andEWPassesGreen_inSameStep() {
        var request = new SimulationRequest(List.of(
                new AddVehicleCommandDto("aggressive-front", Direction.NORTH, Direction.SOUTH, DriverStrategyType.AGGRESSIVE),
                new AddVehicleCommandDto("ew-1", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew-2", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew-3", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new StepCommandDto(),
                new StepCommandDto(),
                new StepCommandDto()
        ));

        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        assertThat(steps.get(0).leftVehicles())
                .as("Step 1: aggressive runs yellow, ew-1 passes green")
                .containsExactlyInAnyOrder("aggressive-front", "ew-1");
        assertThat(steps.get(1).leftVehicles()).containsExactly("ew-2");
        assertThat(steps.get(2).leftVehicles()).containsExactly("ew-3");
    }

    /**
     * PASSIVE driver does NOT pass on YELLOW.
     * Same setup as above but NS vehicle is PASSIVE → stays in queue during yellow step.
     */
    @Test
    void passiveStopsAtYellow_remainsInQueueUntilGreen() {
        var request = new SimulationRequest(List.of(
                new AddVehicleCommandDto("passive-front", Direction.NORTH, Direction.SOUTH, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew-1", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new AddVehicleCommandDto("ew-2", Direction.WEST, Direction.EAST, DriverStrategyType.PASSIVE),
                new StepCommandDto(),
                new StepCommandDto(),
                new StepCommandDto()
        ));

        var steps = service.run(request, new WeightedQueueStrategy()).stepStatuses();

        // Step 1: EW wins (2>1), N=YELLOW → passive-front blocked; ew-1 passes on GREEN
        assertThat(steps.get(0).leftVehicles())
                .as("Step 1: passive blocked at yellow, only ew-1 passes")
                .containsExactly("ew-1");
        // Step 2: EW_GREEN continues → ew-2 passes, passive still waiting
        assertThat(steps.get(1).leftVehicles()).containsExactly("ew-2");
        // Step 3: NS wins (1>0), N=GREEN now → passive-front finally passes
        assertThat(steps.get(2).leftVehicles())
                .as("Step 3: passive-front passes on GREEN after EW phase ends")
                .containsExactly("passive-front");
    }
}
