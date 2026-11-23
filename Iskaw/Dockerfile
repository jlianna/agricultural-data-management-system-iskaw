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
CMD ["sh", "-c", "xvfb-run -a ./target/iskaw-dairyhub-jpro/bin/start.sh -Dhttp.address=0.0.0.0 -Dhttp.port=${PORT:-8080} -Dplay.server.http.address=0.0.0.0 -Dplay.server.http.port=${PORT:-8080}"]