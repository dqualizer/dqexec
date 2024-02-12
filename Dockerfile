# Execution environment (local, ci)
ARG EXECUTION_ENV=local

# User used in local builds to access github packages
ARG GITHUB_USER

# Token used in local builds to access github packages
# or GITHUB_TOKEN in CI
ARG GITHUB_TOKEN

### ----------- Builder Base Image ----------- ###
FROM --platform=$BUILDPLATFORM gradle:8-alpine AS builder-base-image

# Set the working directory to /app
WORKDIR /app
COPY . .

### ----------- Builder CI ----------- ###
FROM builder-base-image as ci-builder
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=$GITHUB_TOKEN


### ----------- Builder LOCAL ----------- ###
FROM builder-base-image as local-builder
ARG GITHUB_USER
ENV GITHUB_USER=$GITHUB_USER
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=$GITHUB_TOKEN

# ensure envsubst
RUN apk update && apk add gettext

# enable dqlang access (no longer necessary?)
#RUN cd gradle \
#    && echo $' allprojects {\n\
#              repositories {\n\
#                  maven {\n\
#                      url =  uri("https://maven.pkg.github.com/dqualizer/dqlang")\n\
#                      credentials {\n\
#                          username = "$GITHUB_USER"\n\
#                          password = "$GITHUB_PACKAGE_READ_TOKEN"\n\
#          		      }\n\
#                  }\n\
#              }\n\
#          } ' > init.gradle \
#    && tmpfile=$(mktemp) \
#    && envsubst < init.gradle > $tmpfile \
#    && mv -f $tmpfile init.gradle


### ----------- Builder Resolver and Executor ----------- ###
FROM ${EXECUTION_ENV}-builder as build-executor
RUN gradle -PgprPassword=$GITHUB_TOKEN -PgprUsername=$GITHUB_USER assemble --no-daemon



### ----------- K6 Builder ----------- ###
# Copied xk6-Dockerfile from: https://github.com/grafana/xk6-output-influxdb/blob/main/Dockerfile
FROM --platform=$BUILDPLATFORM golang:1.20-alpine as k6-builder
WORKDIR $GOPATH/src/go.k6.io/k6

RUN apk --no-cache add git && go install go.k6.io/xk6/cmd/xk6@v0.9.0
RUN xk6 build --with github.com/grafana/xk6-output-influxdb --output /tmp/k6


#### ----------- Runner Definiton ----------- ###
FROM --platform=$BUILDPLATFORM eclipse-temurin:19-jre-alpine

WORKDIR /app

COPY --from=build-executor /app/build/libs/*.jar /app/dqexec.jar
COPY --from=k6-builder /tmp/k6 /usr/bin/k6

VOLUME /app/scripts
VOLUME /app/logging

EXPOSE 8080

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD wget --spider http://localhost:8080/actuator/health || exit 1

# Run the jar file
CMD [ "java", "-jar", "dqexec.jar" ]
