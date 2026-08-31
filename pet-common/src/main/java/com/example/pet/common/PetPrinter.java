package com.example.pet.common;

/**
 * Shared across every pet service, so each service's application model references this module's jar.
 * That jar lives outside the consuming service's own project directory, which is what makes the
 * build cache key checkout-dependent unless the root of the build is a relocation root.
 */
public class PetPrinter {

    public String describe(String petName) {
        System.out.println("Pet: " + petName);
        return "Pet: " + petName;
    }
}
