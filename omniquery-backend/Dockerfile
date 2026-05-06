# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY omniquery-backend/pom.xml ./pom.xml
COPY omniquery-backend/omniquery-api/pom.xml ./omniquery-api/pom.xml
COPY omniquery-backend/omniquery-core/pom.xml ./omniquery-core/pom.xml
COPY omniquery-backend/omniquery-rag/pom.xml ./omniquery-rag/pom.xml
COPY omniquery-backend/omniquery-sdk/pom.xml ./omniquery-sdk/pom.xml
COPY omniquery-backend/omniquery-security/pom.xml ./omniquery-security/pom.xml

COPY omniquery-backend ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl omniquery-api -am package -DskipTests -Dmaven.wagon.http.retryHandler.count=3

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin omniquery
COPY --from=build /workspace/omniquery-api/target/omniquery-api-0.0.1-SNAPSHOT.jar /app/omniquery-api.jar
USER omniquery

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/omniquery-api.jar"]
