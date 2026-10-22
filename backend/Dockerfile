# Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Execucao
FROM eclipse-temurin:25-jre
WORKDIR /app
ENV TZ=America/Sao_Paulo
RUN useradd -r -u 1001 mybarber
COPY --from=build /app/target/mybarber-api.jar app.jar
USER mybarber
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
