FROM gradle:8.14.3-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/garage-flow.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/garage-flow.jar"]
