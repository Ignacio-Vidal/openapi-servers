package com.example;

import com.example.openapi.quarkus.client.api.ApiException;
import com.example.openapi.quarkus.client.api.PetsApi;
import com.example.openapi.quarkus.client.model.*;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import io.quarkus.vertx.runtime.jackson.QuarkusJacksonJsonCodec;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.*;


class PetApiTest {

    static PetsApi petsApi;

    @BeforeAll
    static void setup() {
        ResteasyClient client = new ResteasyClientBuilderImpl().build();
        petsApi = client
                .target(create("http://localhost:8080"))
                .proxy(PetsApi.class);
    }

    @Test
    void createCat_shouldReturnCatResponse() throws ApiException {
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .name("Whiskers")
                .petType(PetRequest.PetTypeEnum.CAT_REQUEST);


        var response = petsApi.createPet(request);

        assertNotNull(response);
        assertInstanceOf(CatResponse.class, response);
        assertEquals(PetResponse.PetTypeEnum.CAT_RESPONSE, response.getPetType());
    }

    @Test
    void createDog_shouldReturnDogResponse() throws ApiException {
        PetRequest request = new DogRequest()
                .breedType(DogBreedType.BULLDOG)
                .trained(true)
                .weightKg(100.0)
                .name("Rover")
                .petType(PetRequest.PetTypeEnum.DOG_REQUEST);

        var response = petsApi.createPet(request);

        assertNotNull(response);
        assertInstanceOf(DogResponse.class, response);
        assertEquals(PetResponse.PetTypeEnum.DOG_RESPONSE, response.getPetType());
    }
}
