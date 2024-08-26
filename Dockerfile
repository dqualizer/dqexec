FROM gradle:8 AS builder
ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /app
COPY . .

RUN gradle -PgprPassword=$GITHUB_TOKEN -PgprUsername=$GITHUB_USER assemble --no-daemon


### ----------- K6 Builder ----------- ###
# Copied xk6-Dockerfile from: https://github.com/grafana/xk6-output-influxdb/blob/main/Dockerfile
FROM golang:1.20-alpine AS k6-builder
WORKDIR $GOPATH/src/go.k6.io/k6

RUN apk --no-cache add git && go install go.k6.io/xk6/cmd/xk6@v0.9.0
RUN xk6 build --with github.com/grafana/xk6-output-influxdb --output /tmp/k6


#### ----------- Runner Definiton ----------- ###
FROM eclipse-temurin:21-jre-alpine AS rt

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/dqexec.jar
COPY --from=k6-builder /tmp/k6 /usr/bin/k6

# k6 script
VOLUME /app/scripts
# k6 logging
VOLUME /app/logging
# ctk experiments
VOLUME /app/generated_experiments
# ctk inputs
VOLUME /app/input_ressources

EXPOSE 8080

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD wget --spider http://localhost:8080/actuator/health || exit 1

# Run the jar file
CMD [ "java", "-jar", "dqexec.jar" ]
