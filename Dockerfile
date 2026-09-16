# Stage 1: Build JAR using Maven 3.9 on Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/secure-file-storage-1.0.0.jar app.jar

# Create directory for simulated encrypted storage
RUN mkdir -p /app/storage/encrypted

# Expose default dynamic port
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
