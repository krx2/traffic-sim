package com.kzebro.trafficsim.controller;

import com.kzebro.trafficsim.dto.SimulationRequest;
import com.kzebro.trafficsim.dto.SimulationResponse;
import com.kzebro.trafficsim.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "Traffic intersection simulation API")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Run a simulation",
            description = """
                    Executes a sequence of commands against a fresh intersection.

                    Supported command types:
                    - **addVehicle** – enqueues a vehicle on a starting road (`north | south | east | west`)
                    - **step** – advances the simulation by one tick; vehicles on green-light roads pass through

                    The active phase is determined by **WeightedQueueStrategy**: the axis with more waiting
                    vehicles gets the green light. Returns one `StepStatus` per `step` command.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "example from spec",
                                    value = """
                                            {
                                              "commands": [
                                                {"type": "addVehicle", "vehicleId": "vehicle1", "startRoad": "south", "endRoad": "north"},
                                                {"type": "addVehicle", "vehicleId": "vehicle2", "startRoad": "north", "endRoad": "south"},
                                                {"type": "step"},
                                                {"type": "step"},
                                                {"type": "addVehicle", "vehicleId": "vehicle3", "startRoad": "west", "endRoad": "south"},
                                                {"type": "addVehicle", "vehicleId": "vehicle4", "startRoad": "west", "endRoad": "south"},
                                                {"type": "step"},
                                                {"type": "step"}
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Simulation completed",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SimulationResponse.class),
                                    examples = @ExampleObject(
                                            name = "expected output",
                                            value = """
                                                    {
                                                      "stepStatuses": [
                                                        {"leftVehicles": ["vehicle1", "vehicle2"]},
                                                        {"leftVehicles": []},
                                                        {"leftVehicles": ["vehicle3"]},
                                                        {"leftVehicles": ["vehicle4"]}
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public SimulationResponse run(@RequestBody SimulationRequest request) {
        return simulationService.run(request);
    }
}
