package com.example;

import com.example.openapi.quarkus.client.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class PetSimulation extends Simulation {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    public static final String PETS = "/pets";

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // --- Request bodies ---

    private static final PetRequest CAT_REQUEST_BODY = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .name("Whiskers")
                .petType(PetRequest.PetTypeEnum.CAT);

    private static final PetRequest DOG_REQUEST_BODY = new DogRequest()
            .breedType(DogBreedType.BULLDOG)
            .trained(true)
            .weightKg(100.0)
            .name("Rover")
            .petType(PetRequest.PetTypeEnum.DOG);

    ScenarioBuilder createCat = scenario("Create Cat")
            .exec(
                    http("POST /pets - Cat")
                            .post(PETS)
                            .body(StringBody(CAT_REQUEST_BODY.toString()))
                            .check(status().is(200))
            );

    ScenarioBuilder createDog = scenario("Create Dog")
            .exec(
                    http("POST /pets - Dog")
                            .post(PETS)
                            .body(StringBody(toJson(DOG_REQUEST_BODY)))
                            .check(status().is(201))
            );

    // --- Mixed workload (randomised Cat/Dog) ---

    ScenarioBuilder mixedPets = scenario("Mixed Pet Creation")
            .randomSwitch().on(
                    percent(60.0).then(
                            exec(http("POST /pets - Cat")
                                    .post(PETS)
                                    .body(StringBody(toJson(CAT_REQUEST_BODY)))
                                    .check(status().is(200))
                                    .check(jsonPath("$.petType").is("CatResponse"))
                            )
                    ),
                    percent(40.0).then(
                            exec(http("POST /pets - Dog")
                                    .post(PETS)
                                    .body(StringBody(toJson(DOG_REQUEST_BODY)))
                                    .check(status().is(201))
                                    .check(jsonPath("$.petType").is("DogResponse"))
                            )
                    )
            );

    {
        setUp(
                // Warm-up: isolated cat and dog scenarios
                createCat.injectOpen(atOnceUsers(1)),
                createDog.injectOpen(atOnceUsers(1)),

                // Main load: mixed workload with ramp-up
                mixedPets.injectOpen(
                        nothingFor(2),                                  // wait for warm-up
                        rampUsersPerSec(1).to(5).during(30),           // ramp from 1 to 20 rps over 30s
                        constantUsersPerSec(5).during(60),             // sustain 20 rps for 60s
                        rampUsersPerSec(5).to(1).during(10)            // ramp down
                )
        ).protocols(httpProtocol)
                .assertions(
                        global().responseTime().max().lt(2000),                // max response time < 2s
                        global().successfulRequests().percent().gt(99.0)       // > 99% success rate
                );
    }


    private static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request body to JSON", e);
        }
    }
}
