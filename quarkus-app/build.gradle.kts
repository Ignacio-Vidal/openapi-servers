plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.31.3"))
    implementation(project(":contracts"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("io.quarkus:quarkus-opentelemetry")
    testImplementation("io.quarkus:quarkus-junit-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.jboss.resteasy:resteasy-client:6.2.15.Final")
    testImplementation("org.jboss.resteasy:resteasy-jackson2-provider:6.2.15.Final")
    testImplementation("org.eclipse.microprofile.rest.client:microprofile-rest-client-api:3.0")

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
