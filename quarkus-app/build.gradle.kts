plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.35.3"))
    implementation(project(":contracts"))
    implementation("io.quarkus:quarkus-oidc")
    testImplementation("io.quarkus:quarkus-test-security")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    // Runtime for the generated @RegisterRestClient interfaces in :contracts.
    // Required for multipart uploads: RESTEasy Classic only supports multipart via a
    // @MultipartForm POJO and rejects the generated bare @FormParam File parameters
    // with HTTP 415. Note quarkus-rest-client cannot coexist with resteasy-client —
    // Quarkus fails the build with "Mixing Quarkus REST and RESTEasy Classic client parts".
    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("io.quarkus:quarkus-opentelemetry")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit-mockito")
    testImplementation("io.rest-assured:rest-assured")
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

tasks.named<Test>("test") {
    doFirst {
        useJUnitPlatform {
            excludeTags("integration")
        }
    }
}

