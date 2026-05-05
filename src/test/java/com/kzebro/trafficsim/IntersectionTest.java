package com.kzebro.trafficsim;

import com.kzebro.trafficsim.memento.IntersectionMemento;
import com.kzebro.trafficsim.memento.SimulationHistory;
import com.kzebro.trafficsim.model.Direction;
import com.kzebro.trafficsim.model.Intersection;
import com.kzebro.trafficsim.model.LaneType;
import com.kzebro.trafficsim.model.LightPhase;
import com.kzebro.trafficsim.model.Vehicle;
import com.kzebro.trafficsim.model.light.LightColor;
import com.kzebro.trafficsim.strategy.AggressiveDriver;
import com.kzebro.trafficsim.strategy.PassiveDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.kzebro.trafficsim.model.Direction.*;
import static com.kzebro.trafficsim.model.LaneType.*;
import static org.assertj.core.api.Assertions.assertThat;

class IntersectionTest {

    private Intersection intersection;

    @BeforeEach
    void setUp() {
        intersection = new Intersection();
    }

    // ---- Initial state ----

    @Test
    void initialPhase_isNsStraightRight() {
        assertThat(intersection.getCurrentPhase()).isEqualTo(LightPhase.NS_STRAIGHT_RIGHT);
    }

    @Test
    void initialLights_northSouthStraightRight_green_leftAndEW_red() {
        assertLane(NORTH, STRAIGHT,    LightColor.GREEN);
        assertLane(NORTH, RIGHT_TURN,  LightColor.GREEN);
        assertLane(NORTH, LEFT_TURN,   LightColor.RED);
        assertLane(SOUTH, STRAIGHT,    LightColor.GREEN);
        assertLane(SOUTH, RIGHT_TURN,  LightColor.GREEN);
        assertLane(SOUTH, LEFT_TURN,   LightColor.RED);
        assertLane(EAST,  STRAIGHT,    LightColor.RED);
        assertLane(EAST,  RIGHT_TURN,  LightColor.RED);
        assertLane(EAST,  LEFT_TURN,   LightColor.RED);
        assertLane(WEST,  STRAIGHT,    LightColor.RED);
        assertLane(WEST,  RIGHT_TURN,  LightColor.RED);
        assertLane(WEST,  LEFT_TURN,   LightColor.RED);
    }

    // ---- executeStep: basic vehicle movement ----

    @Test
    void executeStep_greenLaneReleasesFirstVehicle() {
        // NORTH → SOUTH = STRAIGHT lane, GREEN in NS_STRAIGHT_RIGHT
        Vehicle v = passive("v1", NORTH, SOUTH);
        intersection.addVehicle(v);

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).containsExactly(v);
        assertThat(intersection.getQueueSize(NORTH)).isZero();
    }

    @Test
    void executeStep_redLaneKeepsVehicle() {
        // WEST → EAST = STRAIGHT lane on WEST, RED initially
        intersection.addVehicle(passive("v1", WEST, EAST));

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).isEmpty();
        assertThat(intersection.getQueueSize(WEST)).isEqualTo(1);
    }

    @Test
    void executeStep_onlyFrontVehiclePassesPerLanePerStep() {
        Vehicle v1 = passive("v1", NORTH, SOUTH);
        Vehicle v2 = passive("v2", NORTH, SOUTH);
        intersection.addVehicle(v1);
        intersection.addVehicle(v2);

        assertThat(intersection.executeStep()).containsExactly(v1);
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);

        assertThat(intersection.executeStep()).containsExactly(v2);
        assertThat(intersection.getQueueSize(NORTH)).isZero();
    }

    @Test
    void executeStep_northAndSouthStraightPassSimultaneously() {
        Vehicle vn = passive("vn", NORTH, SOUTH);
        Vehicle vs = passive("vs", SOUTH, NORTH);
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

    @Test
    void executeStep_rightTurnVehicle_passesOnNsPhase() {
        // NORTH → WEST = RIGHT_TURN lane, GREEN in NS_STRAIGHT_RIGHT
        Vehicle v = passive("v1", NORTH, WEST);
        intersection.addVehicle(v);

        assertThat(intersection.executeStep()).containsExactly(v);
    }

    @Test
    void executeStep_leftTurnVehicle_blockedInNsStraightRightPhase() {
        // NORTH → EAST = LEFT_TURN lane, RED in NS_STRAIGHT_RIGHT
        Vehicle v = passive("v1", NORTH, EAST);
        intersection.addVehicle(v);

        assertThat(intersection.executeStep()).isEmpty();
        assertThat(intersection.getLaneQueueSize(NORTH, LEFT_TURN)).isEqualTo(1);
    }

    // ---- setPhase: transitions ----

    @Test
    void setPhase_samePhase_doesNothing() {
        intersection.setPhase(LightPhase.NS_STRAIGHT_RIGHT);

        assertLane(NORTH, STRAIGHT,   LightColor.GREEN);
        assertLane(EAST,  STRAIGHT,   LightColor.RED);
        assertThat(intersection.getStepsInCurrentPhase()).isZero();
    }

    @Test
    void setPhase_toNsLeft_straightRightBecomesYellow_leftBecomesGreen() {
        intersection.setPhase(LightPhase.NS_LEFT);

        assertLane(NORTH, STRAIGHT,   LightColor.YELLOW);
        assertLane(NORTH, RIGHT_TURN, LightColor.YELLOW);
        assertLane(NORTH, LEFT_TURN,  LightColor.GREEN);
        assertLane(SOUTH, LEFT_TURN,  LightColor.GREEN);
        assertLane(EAST,  STRAIGHT,   LightColor.RED);
        assertLane(WEST,  STRAIGHT,   LightColor.RED);
    }

    @Test
    void setPhase_toEwStraightRight_nsBecomesYellow_ewBecomesGreen() {
        intersection.setPhase(LightPhase.EW_STRAIGHT_RIGHT);

        assertLane(NORTH, STRAIGHT,   LightColor.YELLOW);
        assertLane(NORTH, RIGHT_TURN, LightColor.YELLOW);
        assertLane(EAST,  STRAIGHT,   LightColor.GREEN);
        assertLane(EAST,  RIGHT_TURN, LightColor.GREEN);
        assertLane(EAST,  LEFT_TURN,  LightColor.RED);
    }

    @Test
    void setPhase_differentPhase_resetsStepsInPhase() {
        intersection.executeStep();
        intersection.executeStep();
        assertThat(intersection.getStepsInCurrentPhase()).isEqualTo(2);

        intersection.setPhase(LightPhase.EW_STRAIGHT_RIGHT);
        assertThat(intersection.getStepsInCurrentPhase()).isZero();
    }

    @Test
    void executeStep_afterPhaseSwitch_yellowFinalizesToRed() {
        intersection.setPhase(LightPhase.EW_STRAIGHT_RIGHT);  // NS STRAIGHT/RIGHT → YELLOW
        intersection.executeStep();

        assertLane(NORTH, STRAIGHT,   LightColor.RED);
        assertLane(NORTH, RIGHT_TURN, LightColor.RED);
        assertLane(SOUTH, STRAIGHT,   LightColor.RED);
    }

    // ---- Yellow light + DriverStrategy ----

    @Test
    void aggressiveDriver_passesOnYellowDuringPhaseTransition() {
        intersection.addVehicle(aggressive("ag", NORTH, SOUTH));  // NORTH STRAIGHT
        intersection.setPhase(LightPhase.EW_STRAIGHT_RIGHT);      // NORTH STRAIGHT → YELLOW

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).extracting(Vehicle::id).containsExactly("ag");
    }

    @Test
    void passiveDriver_blockedOnYellowDuringPhaseTransition() {
        intersection.addVehicle(passive("pa", NORTH, SOUTH));     // NORTH STRAIGHT
        intersection.setPhase(LightPhase.EW_STRAIGHT_RIGHT);      // NORTH STRAIGHT → YELLOW

        List<Vehicle> left = intersection.executeStep();

        assertThat(left).isEmpty();
        assertThat(intersection.getLaneQueueSize(NORTH, STRAIGHT)).isEqualTo(1);
    }

    // ---- Memento ----

    @Test
    void saveMemento_capturesCurrentState() {
        intersection.addVehicle(passive("vn", NORTH, SOUTH));  // NORTH STRAIGHT
        intersection.addVehicle(passive("vw", WEST,  EAST));   // WEST STRAIGHT

        IntersectionMemento memento = intersection.saveMemento();

        assertThat(memento.phase()).isEqualTo(LightPhase.NS_STRAIGHT_RIGHT);
        assertThat(memento.vehicleQueues().get(NORTH).get(STRAIGHT)).hasSize(1);
        assertThat(memento.vehicleQueues().get(WEST).get(STRAIGHT)).hasSize(1);
    }

    @Test
    void restoreMemento_restoresPreviousState() {
        intersection.addVehicle(passive("v1", NORTH, SOUTH));
        IntersectionMemento before = intersection.saveMemento();

        intersection.executeStep();   // v1 leaves
        assertThat(intersection.getQueueSize(NORTH)).isZero();

        intersection.restoreMemento(before);

        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);
        assertThat(intersection.getCurrentPhase()).isEqualTo(LightPhase.NS_STRAIGHT_RIGHT);
    }

    @Test
    void simulationHistory_savesSnapshotAfterEachStep() {
        SimulationHistory history = new SimulationHistory();
        intersection.addObserver(history);
        intersection.addVehicle(passive("v1", NORTH, SOUTH));

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
        intersection.addVehicle(passive("v1", NORTH, SOUTH));
        intersection.addVehicle(passive("v2", NORTH, SOUTH));

        intersection.executeStep();  // v1 leaves
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(1);

        history.undo(intersection);
        assertThat(intersection.getQueueSize(NORTH)).isEqualTo(2);
    }

    // ---- Helpers ----

    private Vehicle passive(String id, Direction start, Direction end) {
        return new Vehicle(id, start, end, new PassiveDriver());
    }

    private Vehicle aggressive(String id, Direction start, Direction end) {
        return new Vehicle(id, start, end, new AggressiveDriver());
    }

    private void assertLane(Direction dir, LaneType laneType, LightColor expected) {
        LightColor actual = intersection.getRoads().get(dir).getLane(laneType).getTrafficLight().getColor();
        assertThat(actual).as("Light on %s/%s should be %s", dir, laneType, expected).isEqualTo(expected);
    }
}
