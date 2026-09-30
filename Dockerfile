FROM eclipse-temurin:25 AS build

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies

COPY src ./src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25.0.4.1_1-jre-ubi10-minimal

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
