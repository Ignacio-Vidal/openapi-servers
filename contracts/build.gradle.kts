plugins {
    id("java-library")
    id("org.openapi.generator") version "7.25.0-SNAPSHOT"
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
    implementation("org.eclipse.microprofile.openapi:microprofile-openapi-api:4.1.1")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
}

val generateQuarkusServer = tasks.register("generateQuarkusServer", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")
    library.set("quarkus")


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
            "implicitHeaders" to "true",
            "interfaceOnly" to "true",
            "legacyDiscriminatorBehavior" to "false",
            "openApiNullable" to "false",
            "returnResponse" to "false",
            "serializableModel" to "false",
            "useBeanValidation" to "true",
            "useJakartaEe" to "true",
            "useMicroProfileOpenAPIAnnotations" to "false",
            "useSwaggerAnnotations" to "false",
            "useTags" to "true",
            "useJakartaSecurityAnnotations" to "true",
            "useOneOfInterfaces" to "true",
            "useSealed" to "true",
            "useRecords" to "true",
        )
    )
}

val generateQuarkusServerAlternative = tasks.register("generateQuarkusServerAlternative", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")
    library.set("quarkus")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("$buildDir/generated-sources/openapi/quarkus-server-alternative")
    apiPackage.set("com.example.openapi.quarkus.server.alternative.api")
    modelPackage.set("com.example.openapi.quarkus.server.alternative.model")

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
            "openApiNullable" to "false",
            "returnResponse" to "false",
            "serializableModel" to "false",
            "useBeanValidation" to "true",
            "useJakartaEe" to "true",
            "useMicroProfileOpenAPIAnnotations" to "false",
            "useSwaggerAnnotations" to "false",
            "useTags" to "true",
            "useJakartaSecurityAnnotations" to "true",
            // 7.24.0-SNAPSHOT (pr2-jaxrs-usesealed): oneOf schemas render as sealed interfaces.
            "useOneOfInterfaces" to "false",
            "useSealed" to "true",
            "useRecords" to "true"
        )
    )
}



val generateQuarkusServerRecords = tasks.register("generateQuarkusServerRecords", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates JAX-RS spec server stubs whose oneOf-interface implementations are Java records."
    generatorName.set("jaxrs-spec")
    library.set("quarkus")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("$buildDir/generated-sources/openapi/quarkus-server-records")
    apiPackage.set("com.example.openapi.quarkus.server.records.api")
    modelPackage.set("com.example.openapi.quarkus.server.records.model")

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
            "openApiNullable" to "false",
            "returnResponse" to "false",
            "serializableModel" to "false",
            "useBeanValidation" to "true",
            "useJakartaEe" to "true",
            "useMicroProfileOpenAPIAnnotations" to "false",
            "useSwaggerAnnotations" to "false",
            "useTags" to "true",
            "useJakartaSecurityAnnotations" to "true",
            // 7.24.0-SNAPSHOT (pr3-jaxrs-userecords): oneOf-interface implementations render as
            // Java records. useRecords requires useOneOfInterfaces; combined with useSealed the
            // interface is sealed and permits the record subtypes.
            "useOneOfInterfaces" to "true",
            "useSealed" to "true",
            "useRecords" to "true",
        )
    )
}

val generateQuarkusServerJbossResponse = tasks.register("generateQuarkusServerJbossResponse", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group= "openapi tools"
    description = "Generates OpenAPI server stubs using the JAX-RS specification for Quarkus."
    generatorName.set("jaxrs-spec")
    library.set("quarkus")

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
            "useTags" to "true",

        )
    )
}

val generateQuarkusClient = tasks.register("generateQuarkusClient", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group = "openapi tools"
    description = "Generates a MicroProfile REST client for Quarkus."
    generatorName.set("java")
    library.set("microprofile")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi/quarkus-client")

    // Separate packages so server and client classes never clash
    apiPackage.set("com.example.openapi.quarkus.client.api")
    modelPackage.set("com.example.openapi.quarkus.client.model")
    invokerPackage.set("com.example.openapi.quarkus.client")
    workerIsolation.set("process")

    configOptions.set(
        mapOf(
            // microprofile library generates a @RegisterRestClient interface
            // which Quarkus picks up natively with quarkus-rest-client
            // Quarkus MicroProfile Rest Client version
            "microprofileRestClientVersion" to "3.0",
            // Do not generate/register ApiExceptionMapper. It maps every 4xx/5xx to the
            // generated checked ApiException, which hides the JAX-RS exception hierarchy
            // (BadRequestException, NotAuthorizedException, ForbiddenException, ...) that
            // callers would otherwise get. Both flags are needed: `register` controls the
            // @RegisterProvider on each client interface, `global` controls the @Provider
            // on the mapper itself, which would still apply it without @RegisterProvider.
            "microprofileRegisterExceptionMapper" to "false",
            "microprofileGlobalExceptionMapper" to "false",
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
            "useOneOfDiscriminatorLookup" to "false",
            // microprofile client renders oneOf schemas as interfaces (merged in 7.24).
            // Client sealed support (useSealedOneOfInterfaces — different flag name than
            // jaxrs useSealed) needs the unmerged mp-client-sealed jar; not enabled here.
            "useOneOfInterfaces" to "true",
            "useTags" to "true",
            "useMicroProfileOpenAPIAnnotations" to "true",
            "useSealedOneOfInterfaces" to "true"
        )
    )
}
//
val generateSpringBootServer = tasks.register("generateSpringBootServer", org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    group = "openapi tools"
    description = "Generates OpenAPI server stubs using Spring Boot 4 and Spring 7."
    generatorName.set("spring")

    inputSpec.set("$projectDir/src/main/resources/META-INF/openapi.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi/springboot-server")

    apiPackage.set("com.example.openapi.springboot.server.api")
    modelPackage.set("com.example.openapi.springboot.server.model")

    configOptions.set(
        mapOf(
            "generateSupportingFiles" to "false",
            "generateApis" to "true",
            "generateModels" to "true",
            "annotationLibrary" to "swagger2",
            "bigDecimalAsString" to "true",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false",
            "discriminatorCaseSensitive" to "true",
            "documentationProvider" to "springdoc",
            "generateConstructorWithAllArgs" to "true",
            "implicitHeaders" to "true",
            "interfaceOnly" to "true",
            "legacyDiscriminatorBehavior" to "false",
            "library" to "spring-boot",
            "openApiNullable" to "false",
            "requestMappingMode" to "api_interface",
            "serializableModel" to "false",
            "useSwaggerAnnotations" to "false",
            "useJakartaEe" to "true",
            "useBeanValidation" to "true",
            "useResponseEntity" to "false",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            // Spring renders oneOf interfaces natively, so useOneOfInterfaces works without PR1.
            // Used to verify PR0 resolves the discriminator getter type from the children/base.
            "useOneOfInterfaces" to "true",
        )
    )
}
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
    dependsOn(generateQuarkusServer,generateQuarkusServerAlternative, generateQuarkusServerRecords, generateQuarkusClient,generateQuarkusServerJbossResponse)
}

tasks.named("compileJava") {
    dependsOn(generateQuarkusServer,generateQuarkusServerAlternative, generateQuarkusServerRecords, generateQuarkusClient,generateQuarkusServerJbossResponse
//        , generateSpringBootServer, generateSpringBootClient
    )
}

sourceSets{
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-server/src/gen/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-server-alternative/src/gen/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-server-records/src/gen/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/quarkus-client/src/main/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/springboot-server/src/main/java"))
            srcDir(layout.buildDirectory.dir("generated-sources/openapi/springboot-client/src/main/java"))

        }
    }
}
