FROM maven:3.9.16-eclipse-temurin-21@sha256:80034909d9a9f0b89d3d2c583f07f3b7650309bc4e6b886ad5b860b9e1bfa439 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine@sha256:c788d7d9ddc1cf0cf38a1735e14792eb2ff4dc6886e9658771095b7e36a69523
RUN addgroup -g 10001 risk && adduser -u 10001 -G risk -D risk
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/riskshield-java-fraud-0.1.0-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]
