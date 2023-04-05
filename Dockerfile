FROM gradle:8-alpine AS build

# Set the working directory to /app
WORKDIR /app

ARG GITHUB_USER
ARG GITHUB_PACKAGE_READ_TOKEN

RUN apk update && apk add gettext

# enable dqlang access
RUN echo $' allprojects {\n\
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
    && mv -f $tmpfile init.gradle \
    && realpath init.gradle

RUN cat /app/init.gradle

# Build the project
COPY . .
RUN gradle --init-script init.gradle assemble


FROM eclipse-temurin:19-jre-alpine

# Set the working directory to /app
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Run the jar file
CMD ["java", "-jar", "app.jar"]