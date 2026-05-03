plugins {
    id("java-library")
    id("org.openapi.generator") version "7.23.0-SNAPSHOT"
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
    implementation("io.swagger.core.v3:swagger-annotations:2.2.42")
    implementation("org.springframework:spring-web:7.0.4")
    implementation("org.springframework:spring-context:7.0.4")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
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
            "disableDiscriminatorJsonIgnoreProperties" to "false",
            "disallowAdditionalPropertiesIfNotPresent" to "true",
            "discriminatorCaseSensitive" to "true",
            "generateConstructorWithAllArgs" to "true",
            "implicitHeaders" to "true",            "interfaceOnly" to "true",
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

val generateQuarkusServerJbossResponse = tasks.register("generateQuarkusServerJbossResponse", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("$buildDir/generated-sources/openapi/quarkus-server-jboss-response")
    apiPackage.set("com.example.openapi.quarkus.server.jboss.api")
    modelPackage.set("com.example.openapi.quarkus.server.jboss.model")

    configOptions.set(
        mapOf(
            "bigDecimalAsString" to "true",
            "dateLibrary" to "java8",
            "disableDiscriminatorJsonIgnoreProperties" to "false",
            "disallowAdditionalPropertiesIfNotPresent" to "true",
            "discriminatorCaseSensitive" to "true",
            "generateConstructorWithAllArgs" to "true",
            "implicitHeaders" to "true",
            "interfaceOnly" to "true",
            "legacyDiscriminatorBehavior" to "false",
            "library" to "quarkus",
            "openApiNullable" to "false",
            "returnJBossResponse" to "true",
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
            "disableDiscriminatorJsonIgnoreProperties" to "false",
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
//
//val generateSpringBootServer = tasks.register("generateSpringBootServer", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
//    group = "openapi tools"
//    description = "Generates OpenAPI server stubs using Spring Boot 4 and Spring 7."
//    generatorName.set("spring")
//
//    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
//    outputDir.set("$buildDir/generated-sources/openapi/springboot-server")
//    apiPackage.set("com.example.openapi.springboot.server.api")
//    modelPackage.set("com.example.openapi.springboot.server.model")
//
//    configOptions.set(
//        mapOf(
//            "generateSupportingFiles" to "false",
//            "generateApis" to "true",
//            "generateModels" to "true",
//            "annotationLibrary" to "swagger2",
//            "bigDecimalAsString" to "true",
//            "dateLibrary" to "java8",
//            "disallowAdditionalPropertiesIfNotPresent" to "false",
//            "discriminatorCaseSensitive" to "true",
//            "documentationProvider" to "springdoc",
//            "generateConstructorWithAllArgs" to "true",
//            "implicitHeaders" to "true",
//            "interfaceOnly" to "true",
//            "legacyDiscriminatorBehavior" to "false",
//            "library" to "spring-boot",
//            "openApiNullable" to "false",
//            "requestMappingMode" to "api_interface",
//            "serializableModel" to "false",
//            "useSwaggerAnnotations" to "false",
//            "useJakartaEe" to "true",
//            "useBeanValidation" to "true",
//            "useResponseEntity" to "false",
//            "useSpringBoot3" to "true",
//            "useTags" to "true",
//        )
//    )
//}
//
//val generateSpringBootClient = tasks.register("generateSpringBootClient", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
//    group = "openapi tools"
//    description = "Generates a Spring Boot REST client."
//    generatorName.set("java")
//
//    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
//    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi/springboot-client")
//
//    apiPackage.set("com.example.openapi.springboot.client.api")
//    modelPackage.set("com.example.openapi.springboot.client.model")
//    invokerPackage.set("com.example.openapi.springboot.client")
//
//    generateModelTests.set(false)
//    generateApiTests.set(false)
//
//    configOptions.set(
//        mapOf(
//            "annotationLibrary" to "swagger2",
//            "bigDecimalAsString" to "true",
//            "dateLibrary" to "java8",
//            "disallowAdditionalPropertiesIfNotPresent" to "false",
//            "discriminatorCaseSensitive" to "true",
//            "generateConstructorWithAllArgs" to "true",
//            "implicitHeaders" to "true",
//            "legacyDiscriminatorBehavior" to "false",
//            "library" to "restclient",
//            "openApiNullable" to "false",
//            "serializationLibrary" to "jackson",
//            "serializableModel" to "false",
//            "useBeanValidation" to "true",
//            "useJakartaEe" to "true",
//        )
//    )
//}

tasks.named("jandex") {
    dependsOn(generateQuarkusServer, generateQuarkusClient,generateQuarkusServerJbossResponse)
}

tasks.named("compileJava") {
    dependsOn(generateQuarkusServer, generateQuarkusClient,generateQuarkusServerJbossResponse
//        , generateSpringBootServer, generateSpringBootClient
    )
}

sourceSets{
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-server/src/gen/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-client/src/main/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/springboot-server/src/main/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/springboot-client/src/main/java"))

        }
    }
}
