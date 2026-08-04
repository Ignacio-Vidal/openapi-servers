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

/**
 * Runs the same tests against an already-running application instead of the server @QuarkusTest
 * would start, so the suite can be used to generate traffic against a process you control -- e.g.
 * one launched in dev mode with the debugger attached, to step through a request.
 *
 * Start the app first, then:
 *   ./gradlew :quarkus-app:quarkusDev                       (terminal 1, serves :8080)
 *   ./gradlew :quarkus-app:testAgainstRunningServer         (terminal 2, drives it)
 *
 * Point it elsewhere with -Ptest.baseUri=http://host:port.
 *
 * SecurityApiTest also needs `keycloak.url`, and it must be the SAME Keycloak the running server
 * validates against. Without it the test JVM starts its own Dev Services Keycloak on a random port
 * and mints tokens there -- a different realm with different signing keys -- so the target server
 * rejects every token with 401 and each authenticated test fails. Dev mode pins Keycloak to :9090
 * (`%dev.quarkus.keycloak.devservices.port`), which is the default used here; override with
 * -Pkeycloak.url=http://host:port when the running app uses a different one.
 */
tasks.register<Test>("testAgainstRunningServer") {
    group = "verification"
    description = "Runs the tests against an already-running server (default http://localhost:8080)"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    useJUnitPlatform {
        excludeTags("integration")
    }

    systemProperty("test.base-uri", providers.gradleProperty("test.baseUri").getOrElse("http://localhost:8080"))
    systemProperty("keycloak.url", providers.gradleProperty("keycloak.url").getOrElse("http://localhost:9090"))

    // The target server is external, so its state is not reset between runs; never treat a cached
    // result as valid here.
    outputs.upToDateWhen { false }

    testLogging {
        showStandardStreams = true
    }
}

