# --- Build Stage ---
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Gereken dosyaları kopyala
COPY pom.xml .
COPY src ./src

# JAR üret
RUN mvn clean package -DskipTests

# --- Run Stage ---
FROM openjdk:17-jdk-slim
WORKDIR /app

# Derlenen JAR dosyasını al
COPY --from=build /app/target/payment-service-0.0.1-SNAPSHOT.jar ./payment-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "payment-service.jar"]
