plugins {
    java
    id("io.gatling.gradle") version "3.15.0.1"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    gatlingImplementation(project(":contracts")) {
        isTransitive = false
    }
    gatling("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    gatling("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    gatling("jakarta.validation:jakarta.validation-api:3.1.1")
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
