FROM maven:3.5-jdk-8 as maven
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package --batch-mode

FROM openjdk:8-jre-alpine
COPY --from=maven target/imapcopy-*.jar /imapcopy.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "/imapcopy.jar"]