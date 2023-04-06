
# Execution environment (local, ci)
ARG EXECUTION_ENV=local

# User used in local builds to access github packages
ARG GITHUB_USER

# Token used in local builds to access github packages
# or GITHUB_TOKEN in CI
ARG GITHUB_PACKAGE_READ_TOKEN


FROM gradle:8-alpine AS builder-base-image

# Set the working directory to /app
WORKDIR /app
COPY . .



### ----------- Builder CI ----------- ###
FROM builder-base-image as ci-builder
ARG GITHUB_PACKAGE_READ_TOKEN
ENV GITHUB_TOKEN=$GITHUB_PACKAGE_READ_TOKEN


### ----------- Builder LOCAL ----------- ###
FROM builder-base-image as local-builder
ARG GITHUB_USER
ARG GITHUB_PACKAGE_READ_TOKEN

# ensure envsubst
RUN apk update && apk add gettext

# enable dqlang access
RUN cd gradle \
    && echo $' allprojects {\n\
              repositories {\n\
                  maven {\n\
                      url =  uri("https://maven.pkg.github.com/dqualizer/dqlang")\n\
                      credentials {\n\
                          username = "$GITHUB_USER"\n\
                          password = "$GITHUB_PACKAGE_READ_TOKEN"\n\
          		      }\n\
                  }\n\
              }\n\
          } ' > init.gradle \
    && tmpfile=$(mktemp) \
    && envsubst < init.gradle > $tmpfile \
    && mv -f $tmpfile init.gradle


### ----------- Builder Resolver and Executor ----------- ###
FROM ${EXECUTION_ENV}-builder as build-executor
RUN gradle --init-script gradle/init.gradle assemble


#### ----------- Runner Definiton ----------- ###
FROM eclipse-temurin:19-jre-alpine

# Set the working directory to /app
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build-executor /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Run the jar file
CMD ["java", "-jar", "app.jar"]

