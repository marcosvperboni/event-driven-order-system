# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests \
    && mv target/payment-service-*.jar target/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/app.jar app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh
USER app

EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8082/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["docker-entrypoint.sh"]
