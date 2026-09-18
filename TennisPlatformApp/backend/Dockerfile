# syntax=docker/dockerfile:1

# ---- build stage -------------------------------------------------------------
FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /build

# Dependencies are resolved in their own layer so a source-only change does not
# re-download the world on every rebuild.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Never run the application as root.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
