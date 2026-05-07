package com.example;

import com.example.openapi.quarkus.client.api.ApiException;
import com.example.openapi.quarkus.client.api.PetApi;
import com.example.openapi.quarkus.client.model.*;
import jakarta.ws.rs.BadRequestException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.*;

//@Tag("integration")
class PetApiTest {

    static PetApi petsApi;

    @BeforeAll
    static void setup() {
        int port = Integer.parseInt(System.getProperty("quarkus.http.port", "8080"));
        ResteasyClient client = new ResteasyClientBuilderImpl().build();
        petsApi = client
                .target(create("http://localhost:" + port))
                .proxy(PetApi.class);
    }

    @Test
    void createCat_shouldReturnCatResponse() throws ApiException {
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .name("Whiskers")
                .petType(PetRequest.PetTypeEnum.CAT);


        PetResponse response = petsApi.createPet(request);

        assertNotNull(response);
        assertInstanceOf(CatResponse.class, response);
        assertEquals(PetResponse.PetTypeEnum.CAT, response.getPetType());
    }

    @Test
    void shouldFailForMissingCatProperty(){
        PetRequest request = new CatRequest()
                .petType(PetRequest.PetTypeEnum.CAT);

        assertThrows(BadRequestException.class, () -> petsApi.createPet(request));
    }

    @Test
    void shouldFailForMissingDiscriminator(){
        PetRequest request = new PetRequest();
        assertThrows(BadRequestException.class, () -> petsApi.createPet(request));
    }

    @Test
    void shouldSucceedWithImplicitDiscriminator() throws ApiException {
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.BENGAL)
                .indoor(true)
                .name("Whiskers");

        PetResponse response = petsApi.createPet(request);
        assertNotNull(response);
        assertInstanceOf(CatResponse.class, response);
        assertEquals(PetResponse.PetTypeEnum.CAT, response.getPetType());

    }

    @Test
    void createDog_shouldReturnDogResponse() throws ApiException {
        PetRequest request = new DogRequest()
                .breedType(DogBreedType.BULLDOG)
                .trained(true)
                .weightKg(100.0)
                .name("Rover")
                .petType(PetRequest.PetTypeEnum.DOG);

        var response = petsApi.createPet(request);

        assertNotNull(response);
        assertInstanceOf(DogResponse.class, response);
        assertEquals(PetResponse.PetTypeEnum.DOG, response.getPetType());
    }

    @Test
    void updateCat_shouldReturn204() {
        PetRequest updateRequest = new CatRequest()
                .breedType(CatBreedType.MAINE_COON)
                .indoor(false)
                .name("Mittens Updated")
                .petType(PetRequest.PetTypeEnum.CAT);

        assertDoesNotThrow(() -> petsApi.updatePet(UUID.randomUUID(), updateRequest));
    }

    @Test
    void updateDog_shouldReturn204() {
        PetRequest updateRequest = new DogRequest()
                .breedType(DogBreedType.GOLDEN_RETRIEVER)
                .trained(true)
                .weightKg(25.0)
                .name("Fido Updated")
                .petType(PetRequest.PetTypeEnum.DOG);

        assertDoesNotThrow(() -> petsApi.updatePet(UUID.randomUUID(), updateRequest));
    }

    @Test
    void updatePet_withMissingRequiredFields_shouldReturn400() {
        // Missing name, breedType, indoor — all required fields
        assertThrows(BadRequestException.class,
                () -> petsApi.updatePet(UUID.randomUUID(),
                        new CatRequest().petType(PetRequest.PetTypeEnum.CAT)));
    }


}
