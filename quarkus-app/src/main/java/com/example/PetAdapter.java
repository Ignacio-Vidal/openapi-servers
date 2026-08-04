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
        // PetRequestV2 is a sealed interface permitting only CatRequestV2/DogRequestV2, so this
        // switch is exhaustive without a default branch.
        //
        // TEMPORARY: this method previously used record deconstruction patterns, which require
        // `useRecords`. That option lives on the still-unmerged PR #24188, so against upstream
        // master the V2 models generate as classes and the patterns do not compile. Reverted to
        // type patterns + the all-args constructor for the duration of the file-upload testing;
        // restore from the stash once useRecords is available again.
        return switch (petRequestV2) {
            // The generated constructor covers only the required properties; the optional ones
            // (declawed / weightKg) are applied through the fluent setters.
            case CatRequestV2 cat ->
                    new CatResponseV2(
                            cat.getPetType(),
                            cat.getName(),
                            cat.getBreedType(),
                            cat.getIndoor(),
                            BigDecimal.valueOf(30))
                            .declawed(cat.getDeclawed());
            case DogRequestV2 dog ->
                    new DogResponseV2(
                            dog.getPetType(),
                            dog.getName(),
                            dog.getBreedType(),
                            dog.getTrained())
                            .weightKg(dog.getWeightKg());
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
