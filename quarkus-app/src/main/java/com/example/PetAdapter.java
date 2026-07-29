package com.example;

import com.example.openapi.quarkus.server.api.PetApi;
import com.example.openapi.quarkus.server.model.*;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.UUID;

@RunOnVirtualThread
@ApplicationScoped
public class PetAdapter implements PetApi{
    @Override

    public PetResponse createPet(PetRequest petRequest) {
        // PetRequest is a sealed class permitting only CatRequest and DogRequest. Unlike the V2
        // sealed *interface*, the generated V1 base class is not abstract, so a bare PetRequest is
        // itself a possible input and the permitted subtypes alone are not exhaustive - hence the
        // explicit PetRequest case. Listing it (rather than using `default`) keeps the compile-time
        // exhaustiveness check: a new permitted subtype still breaks the build.
        return switch (petRequest) {
            case CatRequest cat -> new CatResponse()
                    .breedType(CatBreedType.SIAMESE)
                    .declawed(false)
                    .indoor(true)
                    .age(BigDecimal.valueOf(30))
                    .petType(PetResponse.PetTypeEnum.CAT)
                    .name("Whiskers");
            case DogRequest dog -> new DogResponse()
                    .breedType(DogBreedType.BULLDOG)
                    .trained(true)
                    .weightKg(100D)
                    .petType(PetResponse.PetTypeEnum.DOG)
                    .name("Rover");
            case PetRequest base -> throw new IllegalArgumentException(
                    "Unsupported pet type: " + base.getPetType());
        };
    }

    @Override
    public PetResponseV2 createPetV2(PetRequestV2 petRequestV2) {
        // PetRequestV2 is a sealed interface permitting only the CatRequestV2/DogRequestV2 records,
        // so this switch is exhaustive without a default branch. The record patterns deconstruct the
        // request to reuse the submitted name, and the immutable responses are built through their
        // canonical constructors rather than the fluent setters used by the V1 classes.
        return switch (petRequestV2) {
            case CatRequestV2(PetType petType, String name, var breedType, var indoor, var declawed) ->
                    new CatResponseV2(
                            petType,
                            name,
                            breedType,
                            indoor,
                            declawed,
                            BigDecimal.valueOf(30));
            case DogRequestV2(PetType petType, String name, var breedType, var trained, var weightKg) ->
                    new DogResponseV2(
                            petType,
                            name,
                            breedType,
                            trained,
                            weightKg);
        };
    }

    @Override
    public void updatePet(UUID petId, PetRequest petRequest) {
            // No-op for this example
        switch (petRequest) {
            case CatRequest cat -> {
                // Handle cat update logic here
            }
            case DogRequest dog -> {
                // Handle dog update logic here
            }
            case PetRequest base -> throw new IllegalArgumentException(
                    "Unsupported pet type: " + base.getPetType());
        }
    }
}
