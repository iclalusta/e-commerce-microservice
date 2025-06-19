# ---------- Build stage ----------
FROM maven:3-amazoncorretto-21-debian-bookworm AS build
WORKDIR /app

# Copy sources and build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Runtime stage ----------
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the *only* JAR produced by Maven into /app
# (If you ever have more than one, adjust the pattern.)
COPY --from=build /app/target/*.jar ./

# Default port
EXPOSE 8082

# Use shell form so the wildcard is expanded by /bin/sh
ENTRYPOINT ["sh", "-c", "java -jar /app/*.jar"]
