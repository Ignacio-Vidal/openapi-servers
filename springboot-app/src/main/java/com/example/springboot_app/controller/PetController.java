package com.example.springboot_app.controller;

import com.example.openapi.springboot.server.api.PetsApi;
import com.example.openapi.springboot.server.model.*;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PetController implements PetsApi {
    @Override
    public PetResponse createPet(PetRequest petRequest) {
        return switch (petRequest.getPetType()) {
            case CAT -> new CatResponse()
                    .declawed(true)
                    .breedType(CatBreedType.PERSIAN)
                    .indoor(true)
                    .petType(petRequest.getPetType())
                    .name(petRequest.getName());
            case DOG -> new DogResponse()
                    .breedType(DogBreedType.BULLDOG)
                    .trained(true)
                    .petType(petRequest.getPetType())
                    .name(petRequest.getName());
        };
    }
}
