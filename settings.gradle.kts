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
        id("io.quarkus") version "3.31.3"
    }
}

include(":quarkus-app",
//    ":springboot-app",
    ":contracts")
