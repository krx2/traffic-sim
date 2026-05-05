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
        intersection = new Intersection(); // initial phase: NS_STRAIGHT_RIGHT
    }

    // ---- WeightedQueueStrategy ----

    @Test
    void weighted_nsHasVehicles_staysInNsStraightRight() {
        addVehicles(NORTH, SOUTH, 3);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);
    }

    @Test
    void weighted_nsEmpty_noNsLeft_advancesToNsEwClearance() {
        // No vehicles anywhere → NS straight/right empty → skip NS_LEFT → clearance first
        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_EW_CLEARANCE);
    }

    @Test
    void weighted_nsEmptyStraightRight_butNsLeftHasVehicles_advancesToNsLeft() {
        addVehicles(NORTH, EAST, 2); // NORTH→EAST = LEFT_TURN

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_LEFT);
    }

    @Test
    void weighted_inNsLeft_vehiclesPresent_staysInNsLeft() {
        intersection.setPhase(NS_LEFT);
        addVehicles(NORTH, EAST, 1);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_LEFT);
    }

    @Test
    void weighted_inNsLeft_empty_advancesToNsEwClearance() {
        intersection.setPhase(NS_LEFT);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_EW_CLEARANCE);
    }

    @Test
    void weighted_inNsEwClearance_staysFirstStep_thenAdvancesToEwStraightRight() {
        intersection.setPhase(NS_EW_CLEARANCE);
        // stepsInPhase=0 → keep clearance this step
        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_EW_CLEARANCE);

        intersection.executeStep(); // stepsInPhase=1
        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_STRAIGHT_RIGHT);
    }

    @Test
    void weighted_inEwStraightRight_ewHasVehicles_stays() {
        intersection.setPhase(EW_STRAIGHT_RIGHT);
        addVehicles(WEST, EAST, 2);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_STRAIGHT_RIGHT);
    }

    @Test
    void weighted_inEwStraightRight_empty_noEwLeft_advancesToEwNsClearance() {
        intersection.setPhase(EW_STRAIGHT_RIGHT);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_NS_CLEARANCE);
    }

    @Test
    void weighted_inEwStraightRight_empty_ewLeftHasVehicles_advancesToEwLeft() {
        intersection.setPhase(EW_STRAIGHT_RIGHT);
        addVehicles(EAST, SOUTH, 1); // EAST→SOUTH = LEFT_TURN

        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_LEFT);
    }

    @Test
    void weighted_inEwLeft_empty_advancesToEwNsClearance() {
        intersection.setPhase(EW_LEFT);

        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_NS_CLEARANCE);
    }

    @Test
    void weighted_inEwNsClearance_staysFirstStep_thenAdvancesToNsStraightRight() {
        intersection.setPhase(EW_NS_CLEARANCE);
        assertThat(weighted.determinePhase(intersection)).isEqualTo(EW_NS_CLEARANCE);

        intersection.executeStep();
        assertThat(weighted.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);
    }

    // ---- FixedTimeStrategy ----

    @Test
    void fixedTime_belowDuration_keepsCurrentPhase() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(3);
        intersection.executeStep();
        intersection.executeStep();

        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);
    }

    @Test
    void fixedTime_atDuration_advancesToNextPhase() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(3);
        intersection.executeStep();
        intersection.executeStep();
        intersection.executeStep();

        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_LEFT);
    }

    @Test
    void fixedTime_cyclesThroughAllSixPhases() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(1);

        // stepsInPhase=0 < 1 → keep NS_STRAIGHT_RIGHT
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);

        intersection.executeStep();   // stepsInPhase=1 ≥ 1 → next = NS_LEFT
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_LEFT);

        intersection.setPhase(NS_LEFT);
        intersection.executeStep();   // → NS_EW_CLEARANCE
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_EW_CLEARANCE);

        intersection.setPhase(NS_EW_CLEARANCE);
        intersection.executeStep();   // clearance, stepsInPhase=1 ≥ 1 → EW_STRAIGHT_RIGHT
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_STRAIGHT_RIGHT);

        intersection.setPhase(EW_STRAIGHT_RIGHT);
        intersection.executeStep();   // → EW_LEFT
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_LEFT);

        intersection.setPhase(EW_LEFT);
        intersection.executeStep();   // → EW_NS_CLEARANCE
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_NS_CLEARANCE);

        intersection.setPhase(EW_NS_CLEARANCE);
        intersection.executeStep();   // → NS_STRAIGHT_RIGHT (wrap)
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);
    }

    @Test
    void fixedTime_clearanceAlwaysLastsOneStep_ignoringPhaseDuration() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(10); // long duration
        intersection.setPhase(NS_EW_CLEARANCE);

        // stepsInPhase=0 → keep clearance
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_EW_CLEARANCE);

        intersection.executeStep(); // stepsInPhase=1 → advance regardless of phaseDuration=10
        assertThat(strategy.determinePhase(intersection)).isEqualTo(EW_STRAIGHT_RIGHT);
    }

    @Test
    void fixedTime_defaultDuration_isThree() {
        FixedTimeStrategy strategy = new FixedTimeStrategy(); // default = 3
        intersection.executeStep();
        intersection.executeStep();
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_STRAIGHT_RIGHT);

        intersection.executeStep();
        assertThat(strategy.determinePhase(intersection)).isEqualTo(NS_LEFT);
    }

    // ---- Helper ----

    private void addVehicles(Direction from, Direction to, int count) {
        for (int i = 0; i < count; i++) {
            intersection.addVehicle(new Vehicle("v" + from + i, from, to, new PassiveDriver()));
        }
    }
}
