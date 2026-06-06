# 🟢 FIX: Switch to the official, active Eclipse Temurin JDK 21 image
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy your gateway source code into the container
COPY SmartCityGateway.java .

# Compile the code inside the container environment
RUN javac SmartCityGateway.java

# Open up the reactive telemetry network ports
EXPOSE 8080
EXPOSE 9999

# Run the compiled Java gateway
CMD ["java", "SmartCityGateway"]