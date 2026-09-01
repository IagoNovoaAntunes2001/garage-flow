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
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","/app/garage-flow.jar"]
