FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY . .
RUN mvn clean compile -DskipTests
EXPOSE 8080
CMD ["mvn", "jpro:run", "-Djpro.port=8080"]