package com.kzebro.trafficsim;

import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.model.Vehicle;
import com.kzebro.trafficsim.strategy.FixedTimeStrategy;
import com.kzebro.trafficsim.strategy.PassiveDriver;
import com.kzebro.trafficsim.strategy.WeightedQueueStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.kzebro.trafficsim.model.Direction.*;
import static com.kzebro.trafficsim.model.LightPhase.*;
import static org.assertj.core.api.Assertions.assertThat;

class StrategyTest {

    private Intersection intersection;
    private final WeightedQueueStrategy weighted = new WeightedQueueStrategy();

    @BeforeEach
    void setUp() {
        intersection = new Intersection(); // initial phase: NS_GREEN
    }

    // ---- WeightedQueueStrategy ----

    @Test
    void weighted_ewHeavier_switchesToEwGreen() {
        addVehicles(NORTH, 1);
        addVehicles(WEST,  3);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    @Test
    void weighted_nsHeavier_staysNsGreen() {
        addVehicles(NORTH, 3);
        addVehicles(WEST,  1);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_GREEN);
    }

    @Test
    void weighted_equal_keepsCurrentPhase() {
        addVehicles(NORTH, 2);
        addVehicles(WEST,  2);

        // Current is NS_GREEN → tie should keep NS_GREEN (hysteresis)
        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_GREEN);
    }

    @Test
    void weighted_bothEmpty_keepsCurrentPhase() {
        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_GREEN);
    }

    @Test
    void weighted_switchedToEW_nsHeavierLater_switchesBack() {
        intersection.setPhase(EW_GREEN);
        addVehicles(NORTH, 4);
        addVehicles(WEST,  1);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_GREEN);
    }

    @Test
    void weighted_switchedToEW_tieKeepsEwGreen() {
        intersection.setPhase(EW_GREEN);
        addVehicles(NORTH, 2);
        addVehicles(WEST,  2);

        // Current is EW_GREEN → tie should keep EW_GREEN (hysteresis)
        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    @Test
    void weighted_allVehiclesOnSouthAndEast() {
        addVehicles(SOUTH, 3);
        addVehicles(EAST,  5);
        // NS=3, EW=5 → EW wins
        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    // ---- FixedTimeStrategy ----

    @Test
    void fixedTime_belowDuration_keepsCurrentPhase() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(3);
        intersection.executeStep();
        intersection.executeStep();
        // stepsInCurrentPhase == 2 < 3
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_GREEN);
    }

    @Test
    void fixedTime_atDuration_switchesPhase() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(3);
        intersection.executeStep();
        intersection.executeStep();
        intersection.executeStep();
        // stepsInCurrentPhase == 3 >= 3
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    @Test
    void fixedTime_afterReset_countsFromZero() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(2);
        intersection.executeStep();
        intersection.executeStep();
        // stepsInPhase == 2 → should switch
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_GREEN);

        // Simulate the switch that a service would apply
        intersection.setPhase(EW_GREEN);
        intersection.executeStep();
        // stepsInPhase == 1 (reset after switch), 1 < 2 → keep EW_GREEN
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    @Test
    void fixedTime_defaultDuration_isThree() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(); // default = 3
        intersection.executeStep();
        intersection.executeStep();
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_GREEN);

        intersection.executeStep();
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_GREEN);
    }

    // ---- Helper ----

    private void addVehicles(Direction dir, int count) {
        for (int i = 0; i < count; i++) {
            intersection.addVehicle(new Vehicle("v" + dir + i, dir, SOUTH, new PassiveDriver()));
        }
    }
}
