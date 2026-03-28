package com.example;

import com.example.openapi.quarkus.server.api.PetsApi;
import com.example.openapi.quarkus.server.model.*;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;

@RunOnVirtualThread
@ApplicationScoped
public class PetAdapter implements PetsApi {
    @Override
    public PetResponse createPet(PetRequest petRequest) {
        return switch (petRequest.getPetType()) {
            case CAT_REQUEST -> new CatResponse()
                    .breedType(CatBreedType.SIAMESE)
                    .declawed(false)
                    .indoor(true)
                    .petType(PetResponse.PetTypeEnum.CAT_RESPONSE)
                    .name("Whiskers");
            case DOG_REQUEST -> new DogResponse()
                    .breedType(DogBreedType.BULLDOG)
                    .trained(true)
                    .weightKg(100D)
                    .petType(PetResponse.PetTypeEnum.DOG_RESPONSE)
                    .name("Rover");
        };
    }
}
