# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 shoppew \
    && adduser -S -D -H -u 10001 -G shoppew shoppew
COPY --from=build /workspace/target/backend-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
