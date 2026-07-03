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
        return switch (petRequest.getPetType()) {
            case CAT -> new CatResponse()
                    .breedType(CatBreedType.SIAMESE)
                    .declawed(false)
                    .indoor(true)
                    .age(BigDecimal.valueOf(30))
                    .petType(PetResponse.PetTypeEnum.CAT)
                    .name("Whiskers");
            case DOG -> new DogResponse()
                    .breedType(DogBreedType.BULLDOG)
                    .trained(true)
                    .weightKg(100D)
                    .petType(PetResponse.PetTypeEnum.DOG)
                    .name("Rover");
        };
    }

    @Override
    public PetResponseV2 createPetV2(PetRequestV2 petRequestV2) {
        return switch (petRequestV2.getPetType()) {
            case CAT -> new CatResponseV2()
                    .petType(PetType.CAT)
                    .name("Whiskers")
                    .breedType(CatBreedType.SIAMESE)
                    .indoor(true)
                    .declawed(false)
                    .age(BigDecimal.valueOf(30));
            case DOG -> new DogResponseV2()
                    .petType(PetType.DOG)
                    .name("Rover")
                    .breedType(DogBreedType.BULLDOG)
                    .trained(true)
                    .weightKg(100D);
        };
    }

    @Override
    public void updatePet(UUID petId, PetRequest petRequest) {
            // No-op for this example
        switch (petRequest.getPetType()) {
            case CAT -> {

            }
            case DOG -> {
                // Handle dog update logic here
            }
        }
    }
}
