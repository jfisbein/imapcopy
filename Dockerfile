FROM maven:3.8-eclipse-temurin-17 as maven
COPY ./pom.xml ./pom.xml
COPY ./src ./src
RUN mvn package --batch-mode

FROM eclipse-temurin:17
COPY --from=maven target/imapcopy-*.jar /imapcopy.jar
ENTRYPOINT ["java", "-jar", "/imapcopy.jar"]
