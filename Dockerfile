FROM maven:3.6-jdk-11 as maven
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package --batch-mode

FROM openjdk:11-jre-slim
COPY --from=maven target/imapcopy-*.jar /imapcopy.jar
ENTRYPOINT ["java", "-jar", "/imapcopy.jar"]
