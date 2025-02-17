# Étape 1 : Build de l'application
FROM maven:3.8.1-openjdk-17-slim AS build
WORKDIR /app
COPY java/pom.xml .
RUN mvn dependency:go-offline
COPY java/src ./src
RUN mvn package -DskipTests

# Étape 2 : Image d'exécution (on installe Maven pour pouvoir recompiler depuis le conteneur)
FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/GoofyFiles-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
