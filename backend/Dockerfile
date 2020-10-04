FROM gradle:6.6.0-jdk14 AS builder
WORKDIR /app

COPY ./src /app/src
COPY ./build.gradle.kts /app
COPY ./gradle.properties /app

RUN gradle --no-daemon build shadowJar

FROM adoptopenjdk/openjdk14:alpine-jre

RUN apk add --no-cache curl
HEALTHCHECK --interval=10s --timeout=10s --retries=3 CMD curl --silent --fail localhost/api/health || exit 1

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY --from=builder /app/build/libs/book-search.jar /app/book-search.jar
WORKDIR /app

EXPOSE 80

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:InitialRAMPercentage=90", "-XX:MinRAMPercentage=90", "-XX:MaxRAMPercentage=90", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "book-search.jar", "-port", "80"]
