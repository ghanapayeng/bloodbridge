# ==============================================================================
# BloodBridge - Production Container Dockerfile
# ==============================================================================

# ---------- Stage 1: Build Java Application ----------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Maven wrapper and POM first for efficient layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Copy application source code
COPY src src

# Package production executable JAR without running tests during image build
RUN ./mvnw clean package -DskipTests -B

# ---------- Stage 2: Production JRE Runtime ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

# Run as non-root user for security best practices
RUN groupadd -r bloodbridge && useradd -r -g bloodbridge -u 1001 bloodbridge

# Copy packaged fat JAR from build stage
COPY --from=build --chown=bloodbridge:bloodbridge /app/target/*.jar app.jar

USER bloodbridge

# Default environment configuration for ports and addresses
ENV PORT=8080
ENV SERVER_ADDRESS=0.0.0.0

EXPOSE 8080 80

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -jar app.jar --server.port=${PORT:-8080} --server.address=0.0.0.0"]
