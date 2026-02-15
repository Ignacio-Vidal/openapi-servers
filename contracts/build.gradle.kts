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

}

openApiGenerate {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("$buildDir/generated-sources/openapi/quarkus")
    apiPackage.set("com.example.openapi.quarkus.api")
    modelPackage.set("com.example.openapi.quarkus.model")

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
            "useTags" to "true",
        )
    )
}

tasks.named("jandex") {
    dependsOn("openApiGenerate")
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

sourceSets{
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus/src/gen/java"))
        }
    }
}
