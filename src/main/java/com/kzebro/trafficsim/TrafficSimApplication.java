package com.kzebro.trafficsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kzebro.trafficsim.dto.SimulationRequest;
import com.kzebro.trafficsim.dto.SimulationResponse;
import com.kzebro.trafficsim.factory.CommandFactory;
import com.kzebro.trafficsim.service.SimulationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class TrafficSimApplication {

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            runCli(args[0], args[1]);
        } else {
            SpringApplication.run(TrafficSimApplication.class, args);
        }
    }

    private static void runCli(String inputPath, String outputPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimulationRequest request = mapper.readValue(new File(inputPath), SimulationRequest.class);
        SimulationResponse response = new SimulationService(new CommandFactory()).run(request);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), response);
    }
}

