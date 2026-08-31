package com.example.pet.svc3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import com.example.pet.common.PetPrinter;

/**
 * PetPrinter lives in a plain java-library module, so it is not a bean by itself; producing it here
 * keeps the dependency on :pet-common in the application model.
 */
public class PetPrinterProducer {

    @Produces
    @ApplicationScoped
    public PetPrinter petPrinter() {
        return new PetPrinter();
    }
}
