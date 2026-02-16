plugins{
    id("java-library")
    id("org.openapi.generator") version "7.19.0"
    id("org.kordamp.gradle.jandex") version "2.3.0"  // add this

}

group = "com.example"
version = "1.0-SNAPSHOT"

java{
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.31.3"))
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("org.eclipse.microprofile.rest.client:microprofile-rest-client-api:3.0")
}

val generateQuarkusServer = tasks.register("generateQuarkusServer", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("$buildDir/generated-sources/openapi/quarkus-server")
    apiPackage.set("com.example.openapi.quarkus.server.api")
    modelPackage.set("com.example.openapi.quarkus.server.model")

    configOptions.set(
        mapOf(
            "bigDecimalAsString" to "true",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false",
            "discriminatorCaseSensitive" to "true",
            "generateConstructorWithAllArgs" to "true",
            "implicitHeaders" to "true",
            "interfaceOnly" to "true",
            "legacyDiscriminatorBehavior" to "false",
            "library" to "quarkus",
            "openApiNullable" to "false",
            "returnResponse" to "false",
            "serializableModel" to "false",
            "useBeanValidation" to "true",
            "useJakartaEe" to "true",
            "useMicroProfileOpenAPIAnnotations" to "true",
            "useSwaggerAnnotations" to "false",
            "useTags" to "true"
        )
    )
}

val generateQuarkusClient = tasks.register("generateQuarkusClient", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group = "openapi tools"
    description = "Generates a MicroProfile REST client for Quarkus."
    generatorName.set("java")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi/quarkus-client")

    // Separate packages so server and client classes never clash
    apiPackage.set("com.example.openapi.quarkus.client.api")
    modelPackage.set("com.example.openapi.quarkus.client.model")
    invokerPackage.set("com.example.openapi.quarkus.client")

    configOptions.set(
        mapOf(
            // microprofile library generates a @RegisterRestClient interface
            // which Quarkus picks up natively with quarkus-rest-client
            "library" to "microprofile",

            // Quarkus MicroProfile Rest Client version
            "microprofileRestClientVersion" to "3.0",

            "dateLibrary" to "java8",
            "useJakartaEe" to "true",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "disallowAdditionalPropertiesIfNotPresent" to "false",
            "legacyDiscriminatorBehavior" to "false",
            "discriminatorCaseSensitive" to "true",
            "generateConstructorWithAllArgs" to "true",
            "serializableModel" to "false",
            "bigDecimalAsString" to "true",
            "implicitHeaders" to "true",
            "annotationLibrary" to "none",
            "serializationLibrary" to "jackson",
            "useOneOfDiscriminatorLookup" to "false"
        )
    )
}

tasks.named("jandex") {
    dependsOn(generateQuarkusServer, generateQuarkusClient)
}

tasks.named("compileJava") {
    dependsOn(generateQuarkusServer, generateQuarkusClient)
}

sourceSets{
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-server/src/gen/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-client/src/main/java"))
        }
    }
}
