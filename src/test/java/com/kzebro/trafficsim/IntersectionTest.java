package com.kzebro.trafficsim;

import com.kzebro.trafficsim.memento.IntersectionMemento;
import com.kzebro.trafficsim.memento.SimulationHistory;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.model.Vehicle;
import com.kzebro.trafficsim.model.light.LightColor;
import com.kzebro.trafficsim.strategy.AggressiveDriver;
import com.kzebro.trafficsim.strategy.PassiveDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kzebro.trafficsim.model.Direction.*;
import static org.assertj.core.api.Assertions.assertThat;

class IntersectionTest {

    private Intersection intersection;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
    }

    // ---- Initial state ----

    @Test
    void initialPhase_isNsGreen() {
        assertThat(intersection.getCurrentPhase()).isEqualTo(LightPhase.NS_GREEN);
    }

    @Test
    void initialLights_northAndSouthGreen_eastAndWestRed() {
        assertLight(NORTH, LightColor.GREEN);
        assertLight(SOUTH, LightColor.GREEN);
        assertLight(EAST,  LightColor.RED);
        assertLight(WEST,  LightColor.RED);
    }

    // ---- executeStep: basic vehicle movement ----

    @Test
    void executeStep_greenRoadReleasesFirstVehicle() {
        Vehicle v = passive("v1", NORTH);
        intersection.addVehicle(v);

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).containsExactly(v);
        assertThat(intersection.getQueueSize(NORTH)).isZero();
    }

    @Test
    void executeStep_redRoadKeepsVehicle() {
        intersection.addVehicle(passive("v1", WEST)); // WEST is RED initially

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).isEmpty();
        assertThat(intersection.getQueueSize(WEST)).isEqualTo(1);
    }

    @Test
    void executeStep_onlyFrontVehiclePassesPerRoadPerStep() {
        Vehicle v1 = passive("v1", NORTH);
        Vehicle v2 = passive("v2", NORTH);
        intersection.addVehicle(v1);
        intersection.addVehicle(v2);

        assertThat(intersection.executeStep()).containsExactly(v1);
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);

        assertThat(intersection.executeStep()).containsExactly(v2);
        assertThat(intersection.getQueueSize(NORTH)).isZero();
    }

    @Test
    void executeStep_bothNorthAndSouthPassSimultaneously() {
        Vehicle vn = passive("vn", NORTH);
        Vehicle vs = passive("vs", SOUTH);
        intersection.addVehicle(vn);
        intersection.addVehicle(vs);

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).containsExactlyInAnyOrder(vn, vs);
    }

    @Test
    void executeStep_emptyIntersection_returnsEmptyList() {
        assertThat(intersection.executeStep()).isEmpty();
    }

    @Test
    void executeStep_incrementsStepsInCurrentPhase() {
        assertThat(intersection.getStepsInCurrentPhase()).isZero();
        intersection.executeStep();
        assertThat(intersection.getStepsInCurrentPhase()).isEqualTo(1);
        intersection.executeStep();
        assertThat(intersection.getStepsInCurrentPhase()).isEqualTo(2);
    }

    // ---- setPhase: transitions ----

    @Test
    void setPhase_samePhase_doesNothing() {
        intersection.setPhase(LightPhase.NS_GREEN);   // same as initial

        assertLight(NORTH, LightColor.GREEN);
        assertLight(SOUTH, LightColor.GREEN);
        assertLight(EAST,  LightColor.RED);
        assertLight(WEST,  LightColor.RED);
        assertThat(intersection.getStepsInCurrentPhase()).isZero();
    }

    @Test
    void setPhase_differentPhase_outgoingBecomesYellow_incomingBecomesGreen() {
        intersection.setPhase(LightPhase.EW_GREEN);

        assertLight(NORTH, LightColor.YELLOW);
        assertLight(SOUTH, LightColor.YELLOW);
        assertLight(EAST,  LightColor.GREEN);
        assertLight(WEST,  LightColor.GREEN);
    }

    @Test
    void setPhase_differentPhase_resetsStepsInPhase() {
        intersection.executeStep();
        intersection.executeStep();
        assertThat(intersection.getStepsInCurrentPhase()).isEqualTo(2);

        intersection.setPhase(LightPhase.EW_GREEN);
        assertThat(intersection.getStepsInCurrentPhase()).isZero();
    }

    @Test
    void executeStep_afterPhaseSwitch_yellowFinalizesToRed() {
        intersection.setPhase(LightPhase.EW_GREEN);  // N and S become YELLOW
        intersection.executeStep();

        assertLight(NORTH, LightColor.RED);
        assertLight(SOUTH, LightColor.RED);
    }

    // ---- Yellow light + DriverStrategy ----

    @Test
    void aggressiveDriver_passesOnYellowDuringPhaseTransition() {
        intersection.addVehicle(aggressive("ag", NORTH));
        intersection.setPhase(LightPhase.EW_GREEN);  // N → YELLOW

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).extracting(Vehicle::id).containsExactly("ag");
    }

    @Test
    void passiveDriver_blockedOnYellowDuringPhaseTransition() {
        intersection.addVehicle(passive("pa", NORTH));
        intersection.setPhase(LightPhase.EW_GREEN);  // N → YELLOW

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).isEmpty();
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);
    }

    // ---- Memento ----

    @Test
    void saveMemento_capturesCurrentState() {
        Vehicle vn = passive("vn", NORTH);
        intersection.addVehicle(vn);
        intersection.addVehicle(passive("vw", WEST));

        IntersectionMemento memento = intersection.saveMemento();

        assertThat(memento.phase()).isEqualTo(LightPhase.NS_GREEN);
        assertThat(memento.vehicleQueues().get(NORTH)).hasSize(1);
        assertThat(memento.vehicleQueues().get(WEST)).hasSize(1);
    }

    @Test
    void restoreMemento_restoresPreviousState() {
        intersection.addVehicle(passive("v1", NORTH));
        IntersectionMemento before = intersection.saveMemento();

        intersection.executeStep();   // v1 leaves
        assertThat(intersection.getQueueSize(NORTH)).isZero();

        intersection.restoreMemento(before);

        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);
        assertThat(intersection.getCurrentPhase()).isEqualTo(LightPhase.NS_GREEN);
    }

    @Test
    void simulationHistory_savesSnapshotAfterEachStep() {
        SimulationHistory history = new SimulationHistory();
        intersection.addObserver(history);
        intersection.addVehicle(passive("v1", NORTH));

        assertThat(history.size()).isZero();
        intersection.executeStep();
        assertThat(history.size()).isEqualTo(1);
        intersection.executeStep();
        assertThat(history.size()).isEqualTo(2);
    }

    @Test
    void simulationHistory_undoRestoresPreviousSnapshot() {
        SimulationHistory history = new SimulationHistory();
        intersection.addObserver(history);
        intersection.addVehicle(passive("v1", NORTH));
        intersection.addVehicle(passive("v2", NORTH));

        intersection.executeStep();  // v1 leaves, snapshot saved
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);

        history.undo(intersection);  // restore pre-step-1 state
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(2);
    }

    // ---- Helpers ----

    private Vehicle passive(String id, Direction start) {
        return new Vehicle(id, start, SOUTH, new PassiveDriver());
    }

    private Vehicle aggressive(String id, Direction start) {
        return new Vehicle(id, start, SOUTH, new AggressiveDriver());
    }

    private void assertLight(Direction dir, LightColor expected) {
        LightColor actual = intersection.getRoads().get(dir).getTrafficLight().getColor();
        assertThat(actual).as("Light on %s should be %s", dir, expected).isEqualTo(expected);
    }
}
