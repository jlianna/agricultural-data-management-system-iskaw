FROM maven:3.9-eclipse-temurin-21

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgtk-3-0 \
    libgl1 \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    fontconfig \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN mvn clean compile -DskipTests

ENV MAVEN_OPTS="-Xmx128m"
EXPOSE 8080
CMD ["mvn", "jpro:run"]