# syntax = docker/dockerfile:1.0-experimental
FROM maven:3.5-jdk-11 as maven
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package --batch-mode

FROM openjdk:1-jre-slim
COPY --from=maven target/imapcopy-*.jar /imapcopy.jar
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "/imapcopy.jar"]