FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build

# Mantem o download ZIP compativel com o SHA-256 fixado no Maven Wrapper.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
COPY mvnw pom.xml ./
COPY src/main ./src/main
RUN chmod +x mvnw && ./mvnw -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --gid 10001 vacismart \
    && useradd --uid 10001 --gid vacismart --no-create-home \
        --home-dir /app --shell /usr/sbin/nologin vacismart

ENV SPRING_PROFILES_ACTIVE=dev \
    SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080 \
    TZ=America/Sao_Paulo \
    JWT_CHAVE_PUBLICA=file:/run/keys/publica.pem \
    JWT_CHAVE_PRIVADA=file:/run/keys/privada.pem

COPY --from=build --chown=10001:10001 /build/target/*.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
