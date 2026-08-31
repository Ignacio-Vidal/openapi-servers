package com.example.pet.svc4;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.example.pet.common.PetPrinter;

@Path("/pets")
public class PetResource {

    @Inject
    PetPrinter petPrinter;

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Pet create() {
        Pet pet = new Pet("Rex-4", "dog");
        petPrinter.describe(pet.name);
        return pet;
    }
}
