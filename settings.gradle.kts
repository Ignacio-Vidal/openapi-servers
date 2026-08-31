rootProject.name = "openapi-servers"

dependencyResolutionManagement{
        repositories {
            mavenCentral()
            gradlePluginPortal()
            mavenLocal()
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id("io.quarkus") version "3.37.2"
    }
}

include(":quarkus-app",
//    ":springboot-app",
    "performance-test",
    ":contracts")

// Six sibling Quarkus applications plus one shared library, used to measure build cache
// relocation: each service's application model references :pet-common's jar, which lives outside
// the service's own project directory.
include(":pet-common",
    ":pet-service-1",
    ":pet-service-2",
    ":pet-service-3",
    ":pet-service-4",
    ":pet-service-5",
    ":pet-service-6")
