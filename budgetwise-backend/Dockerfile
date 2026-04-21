FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy everything (including pom.xml)
COPY . .

# Build jar
RUN mvn clean package -DskipTests

# ---- RUN APP ----
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
