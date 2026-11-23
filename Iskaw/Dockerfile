FROM maven:3.9-eclipse-temurin-25

RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb \
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
RUN mvn clean jpro:release-dir -DskipTests
RUN chmod +x target/iskaw-dairyhub-jpro/bin/start.sh

EXPOSE 8080
CMD ["xvfb-run", "-a", "./target/iskaw-dairyhub-jpro/bin/start.sh"]