package com.kzebro.trafficsim.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Traffic Intersection Simulator")
                        .version("1.0.0")
                        .description("""
                                REST API for a 4-way traffic intersection simulator.

                                Design patterns used:
                                - **State** — TrafficLight cycles through Green → Yellow → Red states
                                - **Observer** — SimulationHistory listens to intersection steps
                                - **Strategy** — WeightedQueueStrategy / FixedTimeStrategy control phase switching
                                - **Command** — addVehicle and step encapsulated as executable objects
                                - **Memento** — SimulationHistory saves a snapshot after every step
                                - **Factory** — CommandFactory creates commands from JSON DTOs
                                - **Composite** — Intersection contains Roads which contain Vehicles
                                """)
                );
    }
}
