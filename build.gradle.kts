import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dqlangVersion = "3.0.9"

plugins {
  id("org.springframework.boot") version "3.2.1"
  id("io.spring.dependency-management") version "1.1.4"
  kotlin("jvm") version "1.9.0"
  kotlin("plugin.spring") version "1.9.0"

  id("net.researchgate.release") version "3.0.2"
  id("maven-publish")
  id("idea")
  id("eclipse")
  id("com.diffplug.spotless") version "6.24.0"
}



idea { // allows downloading sources and javadoc for IntelliJ with gradle cleanIdea idea
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

eclipse { // allows downloading sources and javadoc for Eclipse with gradle cleanEclipse eclipse
  classpath {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

group = "dqualizer"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

release {
  // no config needed, see https://github.com/researchgate/gradle-release for options
}

publishing {
  repositories {
    maven {
      name = "gpr"
      url = uri("https://maven.pkg.github.com/dqualizer/dqexec")
      credentials(PasswordCredentials::class)
    }
    publications {
      register("jar", MavenPublication::class) {
        from(components["java"])
        pom {
          url.set("https://github.com/dqualizer/dqexec.git")
        }
      }
    }
  }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
  mavenLocal()
  maven {
    name = "gpr"
    url = uri("https://maven.pkg.github.com/dqualizer/dqlang")
    credentials(PasswordCredentials::class)
  }
}

extra["testcontainersVersion"] = "1.17.6"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.data:spring-data-mongodb")
  implementation("org.springframework.plugin:spring-plugin-core:3.0.0")

  implementation("io.github.oshai:kotlin-logging:6.0.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

  implementation("com.github.docker-java:docker-java:3.3.4") {
    exclude(group = "org.slf4j")
  }
  implementation("com.github.docker-java:docker-java-transport-httpclient5:3.2.3")


  implementation("rocks.inspectit.ocelot:inspectit-ocelot-config:SNAPSHOT")

  implementation("org.apache.httpcomponents.core5:httpcore5-h2:5.2.4") //dependency of docker-java


  implementation("io.micrometer:micrometer-tracing-bridge-brave:1.0.3")
  implementation("io.zipkin.reporter2:zipkin-reporter-brave:2.16.3")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
  implementation("org.mapstruct:mapstruct:1.5.3.Final")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.freemarker:freemarker:2.3.31")
  implementation("org.springframework.vault:spring-vault-core:3.0.2")
  implementation("io.github.dqualizer:dqlang:$dqlangVersion")
  compileOnly("org.projectlombok:lombok:1.18.26")
  runtimeOnly("com.h2database:h2:2.1.214")
  annotationProcessor("org.projectlombok:lombok:1.18.26")

  testImplementation("org.mockito:mockito-core:5.4.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.amqp:spring-rabbit-test")
  testImplementation("org.testcontainers:junit-jupiter:1.17.6")
  testImplementation("org.testcontainers:rabbitmq:1.18.0")
  testImplementation("org.jeasy:easy-random-core:5.0.0")
}

dependencyManagement {
  imports {
    mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "17"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

// Disable generation of "-plain" jar by the Spring Boot plugin
tasks.getByName<Jar>("jar") {
  enabled = false
}
