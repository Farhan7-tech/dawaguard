# Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Run
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 dawaguard && mkdir -p /app/data && chown dawaguard /app/data
COPY --from=build /src/target/dawaguard-*.jar app.jar
USER dawaguard
# The H2 database (CDSCO copy, family cabinets) lives in /app/data: mount a volume there to keep cabinets across deploys.
VOLUME /app/data
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
