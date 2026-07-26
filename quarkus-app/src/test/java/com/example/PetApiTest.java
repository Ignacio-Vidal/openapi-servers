package com.example;

import com.example.openapi.quarkus.client.api.ApiException;
import com.example.openapi.quarkus.client.api.PetApi;
import com.example.openapi.quarkus.client.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

//@Tag("integration")
@QuarkusTest
class PetApiTest extends QuarkusRestClientTestBase {

    PetApi petsApi;

    @BeforeEach
    void setupClient() {
        petsApi = client(PetApi.class);
    }

    @Test
    void createCat_shouldReturnCatResponse() throws ApiException {
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .ownerEmail("owner@example.com")
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

        assertFailsWithStatus(400, () -> petsApi.createPet(request));
    }

    @Test
    void shouldFailForMissingDiscriminator(){
        PetRequest request = new PetRequest();
        assertFailsWithStatus(400, () -> petsApi.createPet(request));
    }

    @Test
    void shouldSucceedWithImplicitDiscriminator() throws ApiException {
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.BENGAL)
                .indoor(true)
                .ownerEmail("owner@example.com")
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
                .ownerEmail("owner@example.com")
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
        // Missing name, breedType, indoor, ownerEmail — all required fields
        assertFailsWithStatus(400,
                () -> petsApi.updatePet(UUID.randomUUID(),
                        new CatRequest().petType(PetRequest.PetTypeEnum.CAT)));
    }

    @Test
    void createCat_missingRequiredOwnerEmail_shouldReturn400() {
        // ownerEmail is required on CatRequest but omitted; every other required field is present,
        // so this isolates the @NotNull on ownerEmail as the sole cause of the 400.
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .name("Whiskers")
                .petType(PetRequest.PetTypeEnum.CAT);

        assertFailsWithStatus(400, () -> petsApi.createPet(request));
    }

    @Test
    void createCat_invalidOwnerEmailFormat_shouldReturn400() {
        // ownerEmail has format: email → @Email on the server; a malformed value is rejected
        // even though all required fields are populated.
        PetRequest request = new CatRequest()
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .ownerEmail("not-an-email")
                .name("Whiskers")
                .petType(PetRequest.PetTypeEnum.CAT);

        assertFailsWithStatus(400, () -> petsApi.createPet(request));
    }

    // ── V2: oneOf rendered as interfaces (useOneOfInterfaces) ────────────────
    // PetRequestV2/PetResponseV2 are interfaces; CatRequestV2 implements PetRequestV2,
    // so a typed subtype can be passed directly to the interface-typed operation.

    @Test
    void createPetV2_cat_shouldReturnCatResponseV2() throws ApiException {
        PetRequestV2 request = new CatRequestV2()
                .petType(PetType.CAT)
                .name("Whiskers")
                .breedType(CatBreedType.SIAMESE)
                .indoor(true);

        PetResponseV2 response = petsApi.createPetV2(request);

        assertNotNull(response);
        assertInstanceOf(CatResponseV2.class, response);
        assertEquals(PetType.CAT, response.getPetType());
    }

    @Test
    void createPetV2_dog_shouldReturnDogResponseV2() throws ApiException {
        PetRequestV2 request = new DogRequestV2()
                .petType(PetType.DOG)
                .name("Rover")
                .breedType(DogBreedType.BULLDOG)
                .trained(true);

        PetResponseV2 response = petsApi.createPetV2(request);

        assertNotNull(response);
        assertInstanceOf(DogResponseV2.class, response);
        assertEquals(PetType.DOG, response.getPetType());
    }

    @Test
    void createPetV2_cat_shouldRoundTripAllFields() throws ApiException {
        PetRequestV2 request = new CatRequestV2()
                .petType(PetType.CAT)
                .name("Whiskers")
                .breedType(CatBreedType.SIAMESE)
                .indoor(true)
                .declawed(false);

        PetResponseV2 response = petsApi.createPetV2(request);

        CatResponseV2 cat = assertInstanceOf(CatResponseV2.class, response);
        assertEquals(PetType.CAT, cat.getPetType());
        assertEquals("Whiskers", cat.getName());
        assertEquals(CatBreedType.SIAMESE, cat.getBreedType());
        assertEquals(true, cat.getIndoor());
    }

    @Test
    void createPetV2_dog_shouldRoundTripAllFields() throws ApiException {
        PetRequestV2 request = new DogRequestV2()
                .petType(PetType.DOG)
                .name("Rover")
                .breedType(DogBreedType.BULLDOG)
                .trained(true)
                .weightKg(30.5);

        PetResponseV2 response = petsApi.createPetV2(request);

        DogResponseV2 dog = assertInstanceOf(DogResponseV2.class, response);
        assertEquals(PetType.DOG, dog.getPetType());
        assertEquals("Rover", dog.getName());
        assertEquals(DogBreedType.BULLDOG, dog.getBreedType());
        assertEquals(true, dog.getTrained());
    }

    @Test
    void createPetV2_responseIsExhaustivelyMatchable() throws ApiException {
        PetResponseV2 response = petsApi.createPetV2(
                new CatRequestV2()
                        .petType(PetType.CAT)
                        .name("Whiskers")
                        .breedType(CatBreedType.SIAMESE)
                        .indoor(true));

        // PetResponseV2 is a sealed interface (useSealedOneOfInterfaces), so this pattern switch
        // needs no default branch: the compiler proves CAT and DOG are the only possibilities.
        // If the generator ever stopped emitting `sealed`, this test would fail to compile.
        String sound = switch (response) {
            case CatResponseV2 cat -> "meow from " + cat.getName();
            case DogResponseV2 dog -> "woof from " + dog.getName();
        };

        assertEquals("meow from Whiskers", sound);
    }

    @Test
    void createPetV2_cat_missingRequiredChildProperty_shouldReturn400() {
        // 'indoor' is required on CatRequestV2 but omitted
        PetRequestV2 request = new CatRequestV2()
                .petType(PetType.CAT)
                .name("Whiskers")
                .breedType(CatBreedType.SIAMESE);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    @Test
    void createPetV2_dog_missingRequiredChildProperty_shouldReturn400() {
        // 'trained' is required on DogRequestV2 but omitted
        PetRequestV2 request = new DogRequestV2()
                .petType(PetType.DOG)
                .name("Rover")
                .breedType(DogBreedType.BULLDOG);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    @Test
    void createPetV2_missingRequiredBaseProperty_shouldReturn400() {
        // 'name' is required on the shared PetRequestV2Base but omitted
        PetRequestV2 request = new CatRequestV2()
                .petType(PetType.CAT)
                .breedType(CatBreedType.SIAMESE)
                .indoor(true);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    @Test
    void createPetV2_nameViolatingSizeConstraint_shouldReturn400() {
        // 'name' has @Size(min=1,max=50); a 51-char value violates the upper bound
        PetRequestV2 request = new CatRequestV2()
                .petType(PetType.CAT)
                .name("x".repeat(51))
                .breedType(CatBreedType.SIAMESE)
                .indoor(true);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    @Test
    void createPetV2_dog_weightKgAboveMax_shouldReturn400() {
        // weightKg has @DecimalMax("120.0"); 200.0 is out of range
        PetRequestV2 request = new DogRequestV2()
                .petType(PetType.DOG)
                .name("Rover")
                .breedType(DogBreedType.BULLDOG)
                .trained(true)
                .weightKg(200.0);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    @Test
    void createPetV2_dog_weightKgBelowMin_shouldReturn400() {
        // weightKg has @DecimalMin("0.1"); 0.0 is out of range
        PetRequestV2 request = new DogRequestV2()
                .petType(PetType.DOG)
                .name("Rover")
                .breedType(DogBreedType.BULLDOG)
                .trained(true)
                .weightKg(0.0);

        assertFailsWithStatus(400, () -> petsApi.createPetV2(request));
    }

    // ── V2 raw-JSON: wire-level discriminator handling, independent of the client models ──
    // These post hand-written JSON straight to POST /v2/pets and assert on the raw response
    // body, so they exercise the server's @JsonTypeInfo/@JsonSubTypes machinery directly:
    // the "petType" discriminator must resolve the request to the right concrete subtype, and
    // the polymorphic response must be serialized with the correct discriminator value.

    @Test
    void createPetV2_rawJson_cat_shouldDeserializeAndEchoDiscriminator() {
        String body = """
                {
                  "petType": "CAT",
                  "name": "Whiskers",
                  "breedType": "SIAMESE",
                  "indoor": true
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v2/pets")
                .then()
                .statusCode(201)
                // discriminator must round-trip back on the response subtype
                .body("petType", equalTo("CAT"))
                // a cat-only property proves the server bound the request to CatRequestV2/CatResponseV2
                .body("indoor", equalTo(true));
    }

    @Test
    void createPetV2_rawJson_dog_shouldDeserializeAndEchoDiscriminator() {
        String body = """
                {
                  "petType": "DOG",
                  "name": "Rover",
                  "breedType": "BULLDOG",
                  "trained": true
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v2/pets")
                .then()
                .statusCode(201)
                .body("petType", equalTo("DOG"))
                // a dog-only property proves the server bound the request to DogRequestV2/DogResponseV2
                .body("trained", equalTo(true));
    }

    @Test
    void createPetV2_rawJson_unknownDiscriminator_shouldReturn400() {
        // "FISH" is not a mapped subtype in the @JsonSubTypes table → Jackson cannot resolve it
        String body = """
                {
                  "petType": "FISH",
                  "name": "Nemo"
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/v2/pets")
                .then()
                .statusCode(400);
    }
}
