FROM gradle:8 as builder
ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /app
COPY . .

RUN gradle -PgprPassword=$GITHUB_TOKEN -PgprUsername=$GITHUB_USER assemble --no-daemon


### ----------- K6 Builder ----------- ###
# Copied xk6-Dockerfile from: https://github.com/grafana/xk6-output-influxdb/blob/main/Dockerfile
FROM golang:1.20-alpine as k6-builder
WORKDIR $GOPATH/src/go.k6.io/k6

RUN apk --no-cache add git && go install go.k6.io/xk6/cmd/xk6@v0.9.0
RUN xk6 build --with github.com/grafana/xk6-output-influxdb --output /tmp/k6


#### ----------- Runner Definiton ----------- ###
FROM eclipse-temurin:21-jre-alpine as rt

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/dqexec.jar
COPY --from=k6-builder /tmp/k6 /usr/bin/k6

VOLUME /app/scripts
VOLUME /app/logging

# Run the jar file
CMD [ "java", "-jar", "dqexec.jar" ]
