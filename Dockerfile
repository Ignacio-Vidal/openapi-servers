# Stage 1: Build
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

# Download Pyroscope agent (cached in its own layer)
ADD https://github.com/grafana/pyroscope-java/releases/latest/download/pyroscope.jar /opt/agents/pyroscope.jar

# Copy Gradle wrapper and build files first (for layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY contracts/build.gradle.kts contracts/build.gradle.kts
COPY quarkus-app/build.gradle.kts quarkus-app/build.gradle.kts

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY contracts/ contracts/
COPY quarkus-app/ quarkus-app/
COPY performance-test/ performance-test
# Build the Quarkus uber-jar
RUN ./gradlew :quarkus-app:quarkusBuild --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-noble

ENV LANGUAGE='en_US:en'

COPY --from=build /opt/agents/pyroscope.jar /opt/agents/pyroscope.jar
COPY --from=build /workspace/quarkus-app/build/quarkus-app-1.0.0-SNAPSHOT-runner.jar /deployments/app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Dquarkus.http.host=0.0.0.0", \
    "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
    "-javaagent:/opt/agents/pyroscope.jar", \
    "-jar", "/deployments/app.jar"]
